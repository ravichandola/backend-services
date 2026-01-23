# 💳 Enterprise Multi-Tenant SaaS Backend with Payment Gateway

A complete enterprise-grade multi-tenant SaaS backend system with integrated payment processing, built with **Spring Boot**, **Spring Cloud Gateway**, **PostgreSQL**, **Clerk Authentication**, and **Razorpay**.

## 🎯 What This Project Includes

- ✅ **Multi-Tenant SaaS Architecture** - Organizations, Users, Roles, Memberships
- ✅ **Payment Gateway Integration** - Razorpay payment processing
- ✅ **JWT Authentication** - Clerk-based authentication with API Gateway
- ✅ **Webhook-Driven Identity Sync** - Automatic user/org sync from Clerk
- ✅ **Dockerized Setup** - One command to run everything
- ✅ **Complete Documentation** - Comprehensive guides and references

---

## 🚀 Quick Start

**New to the project?** Start here:

1. **[01_Getting Started](./documentation/01-quick-start/01_GETTING_STARTED.md)** ⭐ - Complete beginner guide
2. **[02_Testing APIs](./documentation/01-quick-start/02_TESTING_APIS.md)** ⭐ - Quick testing guide
3. **[01_Architecture Overview](./documentation/04-architecture/01_ARCHITECTURE.md)** ⭐ - System overview

> **Note:** This project is **fully Dockerized** - no Java, PostgreSQL, or Maven installation required on your local machine.

---

## 🗺️ Documentation Navigation Map

**Follow this path to learn the system:**

```
📚 Documentation
│
├── 🚀 START HERE (New Engineer Path) - Follow 01 → 02 → 03
│   │
│   ├── 01️⃣ Getting Started
│   │   └── [01_GETTING_STARTED.md](./documentation/01-quick-start/01_GETTING_STARTED.md)
│   │
│   ├── 02️⃣ Testing APIs
│   │   └── [02_TESTING_APIS.md](./documentation/01-quick-start/02_TESTING_APIS.md)
│   │
│   └── 03️⃣ Understand Flows
│       └── [03_UNDERSTANDING_FLOWS.md](./documentation/01-quick-start/03_UNDERSTANDING_FLOWS.md)
│
├── 📖 Detailed Documentation - Follow 01 → 02 → 03... in each folder
│   │
│   ├── 📘 02-setup/ (01 → 05)
│   │   ├── [01_QUICK_START.md](./documentation/02-setup/01_QUICK_START.md)
│   │   ├── [02_SETUP_GUIDE.md](./documentation/02-setup/02_SETUP_GUIDE.md)
│   │   ├── [03_DATABASE_SETUP.md](./documentation/02-setup/03_DATABASE_SETUP.md)
│   │   ├── [04_PGADMIN_SETUP.md](./documentation/02-setup/04_PGADMIN_SETUP.md)
│   │   └── [05_SUPABASE_SETUP.md](./documentation/02-setup/05_SUPABASE_SETUP.md)
│   │
│   ├── 📗 03-guides/ (01 → 04)
│   │   ├── [01_API_TESTING_DETAILED.md](./documentation/03-guides/01_API_TESTING_DETAILED.md)
│   │   ├── [02_WEBHOOK_SETUP.md](./documentation/03-guides/02_WEBHOOK_SETUP.md)
│   │   ├── [03_TROUBLESHOOTING.md](./documentation/03-guides/03_TROUBLESHOOTING.md)
│   │   └── [04_FLOW_DETAILED.md](./documentation/03-guides/04_FLOW_DETAILED.md)
│   │
│   └── 🏗️ 04-architecture/ (01 → 02)
│       ├── [01_ARCHITECTURE.md](./documentation/04-architecture/01_ARCHITECTURE.md) - System design
│       ├── [02_IMPLEMENTATION_SUMMARY.md](./documentation/04-architecture/02_IMPLEMENTATION_SUMMARY.md)
│       ├── [03_ARCHITECTURE_DETAILED.md](./documentation/04-architecture/03_ARCHITECTURE_DETAILED.md) - Full details
│       └── fixes/ (01 → 03)
│           ├── [01_MISTAKES_PART1.md](./documentation/04-architecture/fixes/01_MISTAKES_PART1.md)
│           ├── [02_MISTAKES_AND_DESIGN.md](./documentation/04-architecture/fixes/02_MISTAKES_AND_DESIGN.md)
│           └── [03_2026-01-23/](./documentation/04-architecture/fixes/03_2026-01-23/)
│
└── 📚 05-reference/
    ├── [README_ENTERPRISE.md](./documentation/05-reference/README_ENTERPRISE.md)
    └── [HELP.md](./documentation/05-reference/HELP.md)
```

**Quick Decision Tree:**

```
Are you NEW to this project?
│
├─ YES → Go to [documentation/README.md](./documentation/README.md)
│         Follow "Start Here (New Engineer Path)" section
│         Read: 01_Getting Started → 02_Testing APIs → 03_Understanding Flows
│
└─ NO (You know the basics)
    │
    ├─ Need to SETUP? → [02-setup/02_SETUP_GUIDE.md](./documentation/02-setup/02_SETUP_GUIDE.md)
    ├─ Need to TEST? → [03-guides/01_API_TESTING_DETAILED.md](./documentation/03-guides/01_API_TESTING_DETAILED.md)
    ├─ Need to DEBUG? → [03-guides/03_TROUBLESHOOTING.md](./documentation/03-guides/03_TROUBLESHOOTING.md)
    ├─ Need ARCHITECTURE? → [04-architecture/01_ARCHITECTURE.md](./documentation/04-architecture/01_ARCHITECTURE.md)
    └─ Need to LEARN from mistakes? → [04-architecture/fixes/](./documentation/04-architecture/fixes/)
        └── Start with [01_MISTAKES_PART1.md](./documentation/04-architecture/fixes/01_MISTAKES_PART1.md)
```

**📖 Full Documentation Index:** [documentation/README.md](./documentation/README.md)

---

## 🚀 Tech Stack

| Component              | Technology                         |
| :--------------------- | :--------------------------------- |
| **Java**               | 25 (Eclipse Temurin inside Docker) |
| **Spring Boot**        | 4.x                                |
| **Database**           | PostgreSQL 16                      |
| **Containerization**   | Docker & Docker Compose            |
| **Payment Gateway**    | Razorpay (Test Mode)               |
| **Authentication**     | Clerk (JWT-based)                  |
| **API Gateway**        | Spring Cloud Gateway               |

---

## ✨ Key Features

### 🎯 Core Features

1. **Multi-Tenant SaaS** - Organizations, Users, Roles, Memberships
2. **Payment Processing** - Razorpay integration for order creation and verification
3. **JWT Authentication** - Clerk-based authentication via API Gateway
4. **Webhook-Driven Sync** - Automatic identity sync from Clerk webhooks
5. **Dockerized** - Everything runs in containers

### 🏗️ Architecture Highlights

- **API Gateway** validates JWTs and adds trusted headers
- **Backend Service** handles webhooks, business logic, and authorization
- **Payment Service** manages Razorpay integration
- **PostgreSQL** stores all data (multi-tenant schema)

---

## 🖥️ System Requirements

You only need:

- ✅ **Docker Desktop** - [Download](https://www.docker.com/products/docker-desktop)
- ✅ **Git** - Usually pre-installed

> **Note:** ❌ Java, PostgreSQL, and Maven are **not** required on your host machine.

---

## ⚡ Quick Setup (TL;DR)

```bash
# 1. Clone repository
git clone <repository-url>
cd payment

# 2. Create .env file (see documentation for details)
# Add: CLERK_JWKS_URL, CLERK_WEBHOOK_SECRET, RAZORPAY_KEY, RAZORPAY_SECRET

# 3. Start everything
docker-compose up --build

# 4. Wait for services to start (2-5 minutes)
# 5. Test: curl http://localhost:8080/api/health
```

**For detailed setup:** See [01_Getting Started](./documentation/01-quick-start/01_GETTING_STARTED.md)

---

## 📍 Service Access Points

| Service | URL | Credentials |
|---------|-----|------------|
| API Gateway | http://localhost:8080 | - |
| Backend Service | http://localhost:8081 | - |
| Payment Service | http://localhost:8082 | - |
| pgAdmin | http://localhost:5050 | admin@local.com / admin123 |
| PostgreSQL | localhost:5433 | appuser / apppass |

---

## 📁 Project Structure

```
payment/
├── api-gateway/          # Spring Cloud Gateway (JWT validation)
├── backend-service/      # Spring Boot Backend (SaaS model with domain-based structure)
│   └── src/main/java/com/demo/backend/
│       ├── config/       # Shared configuration
│       │   ├── payment/  # Payment service configs (RazorpayConfig)
│       │   └── user/     # User service configs (future)
│       ├── controller/   # REST controllers
│       │   ├── payment/  # PaymentController
│       │   └── user/     # WebhookController, ApiController
│       ├── dto/          # Data Transfer Objects
│       │   └── payment/  # Payment DTOs
│       ├── entity/       # JPA entities
│       │   ├── payment/  # PaymentOrder, PaymentTransaction
│       │   └── user/     # User, Organization, Role, Membership, etc.
│       ├── repository/   # JPA repositories
│       │   ├── payment/  # Payment repositories
│       │   └── user/     # User/Organization repositories
│       ├── service/       # Business logic
│       │   ├── payment/  # PaymentService, PaymentServiceImpl
│       │   └── user/     # WebhookService, AuthorizationService
│       └── exception/     # Custom exceptions
│           └── payment/  # PaymentException
├── payment-service/      # Payment Service (Razorpay integration)
├── docker-compose.yml    # Orchestrates all services
├── .env                  # Environment variables (create this)
└── documentation/        # All documentation (numbered for easy navigation)
    ├── 01-quick-start/   # Beginner guides
    ├── 02-setup/         # Setup guides
    ├── 03-guides/        # How-to guides
    ├── 04-architecture/  # Architecture docs
    └── 05-reference/     # Reference docs
```

---

## 🔧 Configuration

### Environment Variables

Create a `.env` file in the project root:

```env
# Clerk Configuration
CLERK_JWKS_URL=https://your-app.clerk.accounts.dev/.well-known/jwks.json
CLERK_ISSUER=https://your-app.clerk.accounts.dev
CLERK_WEBHOOK_SECRET=whsec_xxxxx

# Payment (Optional)
RAZORPAY_KEY=rzp_test_xxxxx
RAZORPAY_SECRET=xxxxx

# Database (Optional - defaults work for Docker)
SPRING_PROFILES_ACTIVE=dev
```

**For detailed configuration:** See [02-setup/02_SETUP_GUIDE.md](./documentation/02-setup/02_SETUP_GUIDE.md)

---

## 🧪 Quick Test

```bash
# Health check (no auth needed)
curl http://localhost:8080/api/health

# Expected: {"status":"UP","service":"backend-service"}
```

**For full testing guide:** See [02_Testing APIs](./documentation/01-quick-start/02_TESTING_APIS.md)

---

## 📚 Documentation

All documentation is organized in the `documentation/` folder with **numbered files** for easy navigation:

- **Quick Start:** `01-quick-start/` (01 → 03)
- **Setup:** `02-setup/` (01 → 05)
- **Guides:** `03-guides/` (01 → 04)
- **Architecture:** `04-architecture/` (01 → 03)
- **Reference:** `05-reference/`

**📖 Full Documentation Index:** [documentation/README.md](./documentation/README.md)

---

## 🆘 Need Help?

- **New to project?** → [01_Getting Started](./documentation/01-quick-start/01_GETTING_STARTED.md)
- **Setup issues?** → [02_SETUP_GUIDE.md](./documentation/02-setup/02_SETUP_GUIDE.md)
- **Troubleshooting?** → [03_TROUBLESHOOTING.md](./documentation/03-guides/03_TROUBLESHOOTING.md)
- **Architecture questions?** → [01_ARCHITECTURE.md](./documentation/04-architecture/01_ARCHITECTURE.md)
- **Browse all docs?** → [Documentation Index](./documentation/README.md)

---

**Ready to start?** Follow the [Quick Start](#-quick-start) section above! 🚀
