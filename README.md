# 🚗 MyGaadi.com 

[![Java](https://img.shields.io/badge/Java-21_LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-61DAFB?style=for-the-badge&logo=react&logoColor=black)](https://react.dev/)
[![Node.js](https://img.shields.io/badge/Node.js-Express_5-339933?style=for-the-badge&logo=nodedotjs&logoColor=white)](https://nodejs.org/)
[![FastAPI](https://img.shields.io/badge/FastAPI-Python-009688?style=for-the-badge&logo=fastapi&logoColor=white)](https://fastapi.tiangolo.com/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Razorpay](https://img.shields.io/badge/Razorpay-Payment_Gateway-0C2340?style=for-the-badge&logo=razorpay&logoColor=white)](https://razorpay.com/)

---

## 📖 Overview

**MyGaadi.com** is a modern, distributed full-stack used-car marketplace designed for the Indian automotive market. The platform connects verified car sellers with buyers through transparent listings, dedicated role-based portals, secure Razorpay token deposit bookings, and an intelligent AI shopping assistant powered by FastAPI and Groq LLMs.

---

## ✨ Key Features

- **🔍 Smart Car Discovery**: Filter used cars by brand, price range, fuel type (Petrol, Diesel, Electric, CNG), transmission, year, city, and body style.
- **👤 Multi-Role Experience**:
  - **Buyer**: Browse inventory, wishlist favorite cars, book refundable token deposits, and download payment receipts.
  - **Seller**: List cars with multi-image upload, manage vehicle status, and track buyer leads in real time.
  - **Admin**: Dedicated dashboard to approve/reject seller listings, monitor platform metrics, and manage user accounts.
- **💳 Razorpay Payment Integration**: End-to-end token booking workflow with instant order creation, modal checkout popup, signature verification, and automated transaction receipts.
- **🤖 GenAI Car Assistant**: Integrated RAG (Retrieval-Augmented Generation) chatbot answering customer queries about car specifications, maintenance tips, financing, and platform policies.
- **🛡️ Enterprise Security**: Centralized API Gateway featuring JWT access tokens, cryptographically secure refresh token rotation, rate limiting, and role-based route guards.

---

## 🏛️ System Architecture

```text
                        ┌────────────────────────┐
                        │   React 19 Frontend    │
                        │    (Port: 5173)        │
                        └───────────┬────────────┘
                                    │ HTTP / REST
                                    ▼
                        ┌────────────────────────┐
                        │    Node.js Gateway     │
                        │    (Port: 8080)        │
                        │ JWT Auth & Rate Limit  │
                        └─────┬──────┬─────┬─────┘
                              │      │     │
            ┌─────────────────┘      │     └────────────────┐
            ▼                        ▼                      ▼
┌──────────────────────┐ ┌──────────────────────┐ ┌──────────────────┐
│  mygaadi-backend     │ │  payment-service     │ │  genai-service   │
│  (Spring Boot 8081)  │ │  (Spring Boot 8082)  │ │  (FastAPI 8083)  │
│  Cars, Auth, Users   │ │  Razorpay Checkout   │ │  Groq RAG Chat   │
└──────────┬───────────┘ └──────────┬───────────┘ └──────────────────┘
           │                        │
           ▼                        ▼
┌───────────────────────────────────────────────┐
│              MySQL Database (3306)            │
│       mygaadi_db  &  mygaadi_payment_db       │
└───────────────────────────────────────────────┘
```

---

## 📁 Repository Structure

```text
MyGaadi.com/
├── frontend-react/mygaadi-web/    # React 19 SPA (Vite, Bootstrap 5, Axios)
├── gateway-node/                  # Express 5 API Gateway (JWT, RBAC, Proxy)
├── mygaadi-backend/               # Spring Boot 3 Core Service (Auth, Cars, Users)
├── payment-service/               # Spring Boot 3 Payment Microservice (Razorpay)
├── genai-service-fastapi/         # Python FastAPI GenAI Assistant (RAG, Groq LLM)
├── .env.example                   # Master environment template with placeholders
└── .gitignore                     # Comprehensive git ignore rules for secrets and build artifacts
```

---

## 🚀 Quick Start Guide

### Prerequisites

Ensure the following runtimes are installed on your system:
- **Java**: JDK 21 LTS (`java -version`)
- **Node.js**: v20+ & npm (`node -v`, `npm -v`)
- **Python**: 3.11 or 3.12 (`python --version`)
- **MySQL**: 8.0+ running on port `3306` (`mysql -u root -p`)

---

### Step 1: Database Setup

Log into your local MySQL server and create the two required databases:

```sql
CREATE DATABASE IF NOT EXISTS mygaadi_db;
CREATE DATABASE IF NOT EXISTS mygaadi_payment_db;
```

> **Note:** Spring Boot JPA Hibernate automatically creates all tables and constraints on first startup (`ddl-auto=update`). The system also automatically seeds the initial admin user!

---

### Step 2: Configure Environment

Copy the example configuration to `.env` in the root folder:

```bash
cp .env.example .env
```

Open `.env` and set your local MySQL password:
```ini
MYSQL_ROOT_PASSWORD=your_mysql_password
MYSQL_APP_PASSWORD=your_mysql_password
```

*(Optional)* To enable live Razorpay payments or the AI Chatbot, add your keys:
```ini
RAZORPAY_KEY_ID=your_razorpay_key_id
RAZORPAY_KEY_SECRET=your_razorpay_key_secret
GROQ_API_KEY=your_groq_api_key
```

---

### Step 3: Run All Services

Open separate terminal windows and run each service in order:

**Terminal 1 — Core Backend (Spring Boot :8081)**
```bash
cd mygaadi-backend
# Windows:
mvnw.cmd spring-boot:run
# Linux/macOS:
./mvnw spring-boot:run
```

**Terminal 2 — Payment Service (Spring Boot :8082)**
```bash
cd payment-service
# Windows:
mvnw.cmd spring-boot:run
# Linux/macOS:
./mvnw spring-boot:run
```
./mvnw spring-boot:run
```

**Terminal 3 — GenAI Assistant (Port 8083)**
```bash
cd genai-service-fastapi
python -m venv venv
# Windows:
.\venv\Scripts\activate
# Linux/macOS:
source venv/bin/activate

pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8083 --reload
```

**Terminal 4 — API Gateway (Port 8080)**
```bash
cd gateway-node
npm install
node server.js
```

**Terminal 5 — Web Frontend (Port 5173)**
```bash
cd frontend-react/mygaadi-web
npm install
npm run dev
```

---

## 🌐 Port Mapping & Endpoints

| Service | Port | Base URL / Health Endpoint | Responsibility |
| :--- | :--- | :--- | :--- |
| **Frontend Web App** | `5173` | `http://localhost:5173` | React User Interface |
| **API Gateway** | `8080` | `http://localhost:8080/health` | Central Reverse Proxy & Auth Check |
| **Core Backend** | `8081` | `http://localhost:8081/actuator/health` | User Auth, Cars CRUD, Bookings |
| **Payment Service** | `8082` | `http://localhost:8082/actuator/health` | Razorpay Orders, Verify, Receipts |
| **GenAI Assistant** | `8083` | `http://localhost:8083/health` | RAG Car Recommendations & Chat |

---

## 🔑 Default Credentials

The platform includes pre-seeded demo accounts for quick evaluation:

| Portal | Role | Email | Password |
| :--- | :--- | :--- | :--- |
| `/admin/login` | **System Administrator** | `admin@mygaadi.com` | `admin@1234` |
| `/login` | **Car Seller** | `seller1@example.com` | `Seller@1234#` |
| `/login` | **Car Buyer** | `buyer1@example.com` | `Buyer@1234#` |

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
