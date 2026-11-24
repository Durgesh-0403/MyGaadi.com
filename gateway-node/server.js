require("dotenv").config();

const express = require("express");
const helmet = require("helmet");
const morgan = require("morgan");
const { rateLimit } = require("express-rate-limit");
const { createProxyMiddleware } = require("http-proxy-middleware");

const authMiddleware = require("./authMiddleware");

const app = express();
const PORT = process.env.PORT || 8080;

const BACKEND_SERVICE = process.env.BACKEND_SERVICE || "http://localhost:8081";
const PAYMENT_SERVICE = process.env.PAYMENT_SERVICE || "http://localhost:8082";
const GENAI_SERVICE = process.env.GENAI_SERVICE || "http://localhost:8083";

// Trust proxy for rate limit and Caddy reverse proxy headers
app.set("trust proxy", 1);

// 1. Security Header Sanitization Middleware
app.use((req, res, next) => {
  // Strip client-provided internal API keys to prevent impersonation
  delete req.headers["x-internal-api-key"];

  // Block any attempt to access internal service-to-service endpoints directly
  if (req.path.startsWith("/internal") || req.path.startsWith("/api/internal")) {
    return res.status(403).json({
      status: "FAILED",
      message: "Forbidden: Internal service routes cannot be accessed externally"
    });
  }
  next();
});

// 2. Helmet for security headers, allowing cross-origin resource policy for uploaded car images
app.use(
  helmet({
    crossOriginResourcePolicy: { policy: "cross-origin" }
  })
);

// 3. Strict CORS Handling with Preflight Optimization
const rawAllowedOrigins = process.env.FRONTEND_ORIGIN || "http://localhost:3000,http://127.0.0.1:3000";
const allowedOrigins = rawAllowedOrigins.split(",").map((o) => o.trim());

app.use((req, res, next) => {
  const origin = req.headers.origin;

  if (origin && allowedOrigins.includes(origin)) {
    res.setHeader("Access-Control-Allow-Origin", origin);
    res.setHeader("Access-Control-Allow-Credentials", "true");
    res.setHeader("Vary", "Origin");
  }

  // Preflight OPTIONS handler: Return 204 No Content before auth or rate-limiting
  if (req.method === "OPTIONS") {
    res.setHeader(
      "Access-Control-Allow-Methods",
      "GET, POST, PUT, DELETE, PATCH, OPTIONS"
    );
    res.setHeader(
      "Access-Control-Allow-Headers",
      "Content-Type, Authorization, X-Requested-With, Cache-Control"
    );
    res.setHeader("Access-Control-Max-Age", "86400");
    return res.sendStatus(204);
  }

  next();
});

// 4. Rate Limiting for Authentication endpoints
const authLimiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100, // limit each IP to 100 requests per window
  standardHeaders: true,
  legacyHeaders: false,
  message: {
    status: "FAILED",
    message: "Too many authentication requests from this IP, please try again after 15 minutes."
  }
});

app.use("/api/auth/login", authLimiter);
app.use("/api/auth/admin/login", authLimiter);
app.use("/api/auth/register", authLimiter);

// 5. Request Logging (Morgan)
app.use(morgan(process.env.NODE_ENV === "production" ? "combined" : "dev"));

// 6. Gateway Root & Health Status Routes
app.get("/", (req, res) => {
  res.json({
    service: "MyGaadi API Gateway",
    status: "Running",
    timestamp: new Date().toISOString()
  });
});

app.get("/health", (req, res) => {
  res.json({
    status: "UP",
    gateway: "ok",
    timestamp: new Date().toISOString()
  });
});

// 7. Authentication & Coarse RBAC Middleware
app.use(authMiddleware);

// 8. Helper to create proxy options that preserve correct downstream controller paths
function createCustomProxy(target, targetPrefix) {
  return createProxyMiddleware({
    target,
    changeOrigin: true,
    pathRewrite: (path) => {
      // path is the sub-path after Express stripped the mount point
      if (!path || path === "/") {
        return targetPrefix;
      }
      if (path.startsWith("/?")) {
        return targetPrefix + path.substring(1);
      }
      if (path.startsWith("?")) {
        return targetPrefix + path;
      }
      return targetPrefix + (path.startsWith("/") ? path : "/" + path);
    },
    on: {
      proxyRes: (proxyRes) => {
        // Strip downstream CORS headers so only Gateway's single CORS headers are returned
        delete proxyRes.headers["access-control-allow-origin"];
        delete proxyRes.headers["access-control-allow-credentials"];
        delete proxyRes.headers["access-control-allow-methods"];
        delete proxyRes.headers["access-control-allow-headers"];
      },
      error: (err, req, res) => {
        console.error(`[Gateway Proxy Error] target: ${target}, path: ${req.url}:`, err.message);
        if (!res.headersSent) {
          res.status(502).json({
            status: "FAILED",
            message: "Bad Gateway: Downstream service is currently unreachable"
          });
        }
      }
    }
  });
}

// 9. Mount Proxies to Microservices
// NOTE: express.json() is intentionally omitted so multipart uploads and raw webhook payloads pass through intact

// Payment Microservice
app.use("/api/payments", createCustomProxy(PAYMENT_SERVICE, "/payments"));

// GenAI Assistant Microservice
app.use("/api/chat", createCustomProxy(GENAI_SERVICE, "/chat"));

// Backend Microservice Routes
app.use("/api/auth", createCustomProxy(BACKEND_SERVICE, "/auth"));
app.use("/api/cars", createCustomProxy(BACKEND_SERVICE, "/cars"));
app.use("/api/seller", createCustomProxy(BACKEND_SERVICE, "/seller"));
app.use("/api/buyer", createCustomProxy(BACKEND_SERVICE, "/buyer"));
app.use("/api/admin", createCustomProxy(BACKEND_SERVICE, "/admin"));

// Static Uploads from Backend
app.use("/uploads", createCustomProxy(BACKEND_SERVICE, "/uploads"));

// 10. Fallback 404 Handler
app.use((req, res) => {
  res.status(404).json({
    status: "FAILED",
    message: `Route ${req.method} ${req.path} not found on Gateway`
  });
});

// 11. Global Error Handler
app.use((err, req, res, next) => {
  console.error("[Gateway Unhandled Error]:", err);
  res.status(500).json({
    status: "FAILED",
    message: err.message || "Internal Gateway Server Error"
  });
});

// Start listening if run directly
if (require.main === module) {
  app.listen(PORT, () => {
    console.log(`[MyGaadi Gateway] Running on port ${PORT}`);
    console.log(`[MyGaadi Gateway] Routes routed:`);
    console.log(`  - /api/auth, /api/cars, /api/seller, /api/buyer, /api/admin, /uploads -> ${BACKEND_SERVICE}`);
    console.log(`  - /api/payments -> ${PAYMENT_SERVICE}`);
    console.log(`  - /api/chat     -> ${GENAI_SERVICE}`);
  });
}

module.exports = app;
