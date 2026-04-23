<div align="center">

# 🛒 SportShop E-Commerce Platform

**A production-minded, full-featured e-commerce system built with Spring Boot 3.2**

[![CI](https://github.com/toantran00/DA_WEB_CK1_25_26/actions/workflows/ci.yml/badge.svg)](https://github.com/toantran00/DA_WEB_CK1_25_26/actions)
![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.5-6DB33F?logo=spring-boot)
![Spring Security](https://img.shields.io/badge/Spring_Security-JWT_+_Session-6DB33F?logo=spring)
![SQL Server](https://img.shields.io/badge/Database-SQL_Server-CC2927?logo=microsoft-sql-server)
![License](https://img.shields.io/badge/License-Academic-blue)

> Developed as a semester capstone project — HK1 2025–2026.

</div>

---

## ✨ Feature Highlights

| Module | Features |
|--------|----------|
| 🔐 **Authentication** | Form login + JWT hybrid, BCrypt passwords, OTP email verification, forgot-password flow |
| 👥 **Multi-Role System** | ADMIN / VENDOR / SHIPPER / USER with role-based route protection |
| 🛍️ **E-Commerce Core** | Product catalog, categories, cart, multi-store checkout, order tracking |
| 💳 **Payments** | VNPay (sandbox), MoMo (sandbox), QR Code, VietQR bank transfer |
| 💬 **Real-time Chat** | WebSocket-based buyer ↔ vendor messaging |
| 📦 **Order Lifecycle** | Full order state machine: pending → confirmed → shipping → delivered / cancelled |
| 🏷️ **Promotion Engine** | Per-store discount codes with expiry and usage limits |
| 📊 **Excel Import/Export** | Bulk product & user management via Apache POI |
| 📄 **PDF Invoice** | iText-generated invoice with Vietnamese font support |
| 🔖 **QR Code Generation** | ZXing-powered QR codes for payment and product lookup |
| ⏰ **Background Jobs** | Quartz scheduler for OTP cleanup and scheduled tasks |
| 📬 **Email Service** | Gmail SMTP — OTP, order confirmation, reset password |
| 🔍 **Advanced Search** | JPA Specification dynamic filtering (price, category, store, rating) |
| 🏥 **Health Checks** | Spring Boot Actuator at `/actuator/health` |
| 📖 **API Docs** | Swagger UI at `/swagger-ui.html` with JWT auth support |

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Client (Browser)                       │
│              Thymeleaf SSR + REST + WebSocket             │
└──────────────┬───────────────────────────────────────────┘
               │ HTTP / WebSocket
┌──────────────▼───────────────────────────────────────────┐
│                   Spring Boot 3.2                         │
│                                                           │
│  ┌──────────────┐   ┌──────────────┐   ┌──────────────┐ │
│  │  Controllers  │   │   Services   │   │ Repositories │ │
│  │  (MVC + REST) │──▶│  (Business   │──▶│ (Spring Data │ │
│  │               │   │   Logic)     │   │    JPA)      │ │
│  └──────────────┘   └──────────────┘   └──────┬───────┘ │
│                                                │          │
│  ┌──────────────┐   ┌──────────────┐          │          │
│  │ Spring       │   │   Quartz     │          │          │
│  │ Security     │   │  Scheduler   │          │          │
│  │ JWT + Session│   │  (OTP clean) │          │          │
│  └──────────────┘   └──────────────┘          │          │
└───────────────────────────────────────────────┼──────────┘
                                                 │
                          ┌──────────────────────▼───────┐
                          │     SQL Server Database       │
                          │    (DO_AN_WEB1 schema)        │
                          └──────────────────────────────┘
```

### Package Structure

```
src/main/java/vn/iotstar/
├── config/          # Security, WebSocket, OpenAPI, Actuator, Exception Handler
├── controller/      # MVC + REST controllers (admin/, vendor/, shipper/)
├── entity/          # JPA entities (19 domain objects)
├── model/           # DTOs and request/response models (42 classes)
├── repository/      # Spring Data JPA repositories (18 repos)
├── service/         # Service interfaces + implementations
│   └── impl/        # 26 service implementations
├── specification/   # JPA Criteria API dynamic query specs
└── util/            # JwtUtil, QR, PDF helpers
```

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| **Runtime** | Java 17, Spring Boot 3.2.5 |
| **Web** | Spring MVC, Thymeleaf, WebSocket (STOMP) |
| **Security** | Spring Security, JWT (jjwt 0.12.5), BCrypt |
| **Data** | Spring Data JPA, Hibernate, SQL Server |
| **Scheduler** | Quartz 2.x |
| **Payment** | VNPay SDK, MoMo REST API |
| **Files** | Apache POI 5.2 (Excel), iText 5.5 (PDF), ZXing 3.5 (QR) |
| **API Docs** | SpringDoc OpenAPI 2.3 (Swagger UI) |
| **Monitoring** | Spring Boot Actuator |
| **Testing** | JUnit 5, Mockito, Spring Boot Test, H2 (test profile) |
| **Build** | Maven 3.x |

---

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+ (or use the included `mvnw`)
- SQL Server (local or remote) with a database named `DO_AN_WEB1`

### 1. Clone & Configure

```bash
git clone https://github.com/toantran00/DA_WEB_CK1_25_26.git
cd DA_WEB_CK1_25_26
```

Copy the local config template:
```bash
# Windows PowerShell
copy src\main\resources\application-local.properties.example src\main\resources\application-local.properties
```

Set your environment variables (or edit `application.properties` directly for local dev):

```bash
# PowerShell
$env:DB_URL="jdbc:sqlserver://localhost:1433;databaseName=DO_AN_WEB1;encrypt=true;trustServerCertificate=true"
$env:DB_USERNAME="sa"
$env:DB_PASSWORD="your_password"
$env:APP_JWT_SECRET="your-secret-at-least-32-chars-long!!"
$env:MAIL_USERNAME="your_gmail@gmail.com"
$env:MAIL_PASSWORD="your_gmail_app_password"
```

### 2. Run the Application

```bash
# Linux / macOS
./mvnw spring-boot:run

# Windows PowerShell
.\mvnw.cmd spring-boot:run
```

The app starts at **http://localhost:8080**

### 3. Explore

| URL | Description |
|-----|-------------|
| `http://localhost:8080/` | Homepage (product catalog) |
| `http://localhost:8080/login` | Login page |
| `http://localhost:8080/admin/dashboard` | Admin dashboard (requires ADMIN role) |
| `http://localhost:8080/swagger-ui.html` | **Interactive API documentation** |
| `http://localhost:8080/actuator/health` | Health check endpoint |

### Default Seeded Accounts

| Role | Email | Password |
|------|-------|----------|
| ADMIN | admin@sportshop.vn | Admin@123 |
| VENDOR | vendor@sportshop.vn | Vendor@123 |

---

## 🧪 Running Tests

```bash
# Run all tests (uses H2 in-memory, no DB required)
./mvnw test

# Windows
.\mvnw.cmd test
```

Tests run with `@ActiveProfiles("test")` → H2 in-memory database, no external dependencies needed.

**Test coverage includes:**
- `AuthServiceImplTest` — login success/failure, registration, password encoding, account lock
- `DatHangServiceImplTest` — order CRUD, cancellation delivery sync, revenue queries
- `NguoiDungServiceImplTest` — user create/update, lock/unlock, search, email uniqueness
- `JwtUtilTest` — token generation, validation, tamper detection, email extraction

---

## 🌐 Environment Variables Reference

| Variable | Description | Default (dev only) |
|----------|-------------|---------------------|
| `DB_URL` | JDBC connection string | `jdbc:sqlserver://localhost:1433;...` |
| `DB_USERNAME` | Database username | `sa` |
| `DB_PASSWORD` | Database password | *(required)* |
| `APP_JWT_SECRET` | JWT signing secret (≥32 chars) | *(required)* |
| `MAIL_USERNAME` | Gmail address for sending emails | *(optional)* |
| `MAIL_PASSWORD` | Gmail App Password | *(optional)* |
| `VNPAY_TMN_CODE` | VNPay terminal code | `67B6XRS4` (sandbox) |
| `VNPAY_HASH_SECRET` | VNPay hash secret | *(required for payment)* |
| `MOMO_PARTNER_CODE` | MoMo partner code | `MOMOIQA420180417` (sandbox) |
| `MOMO_ACCESS_KEY` | MoMo access key | *(required for payment)* |
| `MOMO_SECRET_KEY` | MoMo secret key | *(required for payment)* |

---

## 🗺️ API Overview (see Swagger UI for full docs)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/auth/login` | Login → returns JWT |
| `POST` | `/api/auth/register` | Register new user |
| `GET` | `/api/cart` | Get current user's cart |
| `POST` | `/api/cart/add` | Add item to cart |
| `POST` | `/api/orders` | Create order from cart |
| `GET` | `/api/orders/{id}` | Get order detail |
| `GET` | `/api/orders/{id}/invoice` | Download PDF invoice |
| `POST` | `/payment/vnpay/create` | Initiate VNPay payment |
| `POST` | `/payment/momo/create` | Initiate MoMo payment |
| `GET` | `/actuator/health` | Health check |

---

## 🔒 Security Notes

- Passwords stored as BCrypt hashes (never plain text)
- JWT tokens signed with HS256, minimum 256-bit secret enforced
- JWT not accepted from query parameters (XSS mitigation)
- File serving validates normalized paths to prevent directory traversal
- Upload endpoints restricted to authenticated users only
- Sensitive config externalized via environment variables
- CSRF disabled for REST API (stateless JWT flow); form-based login uses sessions

---

## 📅 Suggested Next Steps

- [ ] Add integration tests for auth flow and checkout pipeline
- [ ] Introduce Flyway/Liquibase for database migration management  
- [ ] Replace `ddl-auto=update` with `validate` in production profile
- [ ] Deploy to Railway / Render (free tier) for a live demo URL
- [ ] Add rate limiting to auth endpoints (prevent brute force)
- [ ] Implement refresh token mechanism

---

<div align="center">

**Built with ❤️ using Spring Boot 3.2 · Java 17 · SQL Server**

*Semester capstone project — Faculty of Information Technology, HK1 2025–2026*

</div>
