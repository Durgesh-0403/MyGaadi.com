const jwt = require("jsonwebtoken");

const PUBLIC_EXACT_ROUTES = new Set([
  "/",
  "/health",
  "/api/auth/login",
  "/api/auth/admin/login",
  "/api/auth/register/buyer",
  "/api/auth/register/seller",
  "/api/auth/refresh",
  "/api/auth/admin/refresh",
  "/api/auth/logout",
  "/api/chat",
  "/api/payments/webhook"
]);

const PUBLIC_PREFIX_ROUTES = [
  "/uploads/",
  "/api/cars"
];

function isPublicRoute(req) {
  if (req.method === "OPTIONS") {
    return true;
  }

  const path = req.path.replace(/\/+$/, "") || "/";

  if (PUBLIC_EXACT_ROUTES.has(path) || path.startsWith("/api/auth/register/")) {
    return true;
  }

  // Allow GET /api/cars and GET /api/cars/* for public car search & browsing
  if (req.method === "GET") {
    for (const prefix of PUBLIC_PREFIX_ROUTES) {
      if (path === prefix || path.startsWith(prefix + "/") || path.startsWith(prefix + "?")) {
        return true;
      }
    }
  }

  // /uploads are static files, always publicly readable
  if (path.startsWith("/uploads/")) {
    return true;
  }

  return false;
}

module.exports = function authMiddleware(req, res, next) {
  if (isPublicRoute(req)) {
    return next();
  }

  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith("Bearer ")) {
    return res.status(401).json({
      status: "FAILED",
      message: "Authorization header missing or invalid format (Bearer token required)"
    });
  }

  const token = authHeader.substring(7).trim();
  const DEV_DEFAULT_JWT_SECRET = "mygaadi_super_secure_jwt_secret_key_for_development_purposes_only_at_least_64_characters_long_1234567890";
  const secret = process.env.JWT_SECRET || DEV_DEFAULT_JWT_SECRET;

  try {
    const decoded = jwt.verify(token, secret);
    req.user = decoded;

    // Normalize user attributes from token claims
    const userId = decoded.user_id || decoded.id || decoded.userId || "";
    const userRole = decoded.user_role || decoded.role || decoded.userRole || "";
    const userEmail = decoded.sub || decoded.email || "";

    // Forward normalized user claims to downstream services
    req.headers["x-user-id"] = String(userId);
    req.headers["x-user-role"] = String(userRole);
    req.headers["x-user-email"] = String(userEmail);

    // Coarse Role-Based Access Control at Gateway level
    const path = req.path;

    if (path.startsWith("/api/admin") || path.startsWith("/api/payments/admin")) {
      if (userRole !== "ROLE_ADMIN") {
        return res.status(403).json({
          status: "FAILED",
          message: "Forbidden: Administrator role required"
        });
      }
    } else if (path.startsWith("/api/seller")) {
      if (userRole !== "ROLE_SELLER") {
        return res.status(403).json({
          status: "FAILED",
          message: "Forbidden: Seller role required"
        });
      }
    } else if (path.startsWith("/api/buyer")) {
      if (userRole !== "ROLE_BUYER") {
        return res.status(403).json({
          status: "FAILED",
          message: "Forbidden: Buyer role required"
        });
      }
    }

    next();
  } catch (err) {
    if (err.name === "TokenExpiredError") {
      return res.status(401).json({
        status: "FAILED",
        message: "JWT token has expired"
      });
    }
    return res.status(401).json({
      status: "FAILED",
      message: "Invalid JWT token"
    });
  }
};
