# 🛒 E-Commerce Microservices - Payment Service

[![Java](https://img.shields.io/badge/Java-21-orange?style=flat&logo=openjdk)](https://www.java.com/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.1-brightgreen?style=flat&logo=spring-boot)](https://spring.io/projects/spring-boot)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-Enabled-black?style=flat&logo=apache-kafka)](https://kafka.apache.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.3-blue?style=flat&logo=mysql)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-7.2-red?style=flat&logo=redis)](https://redis.io/)
![Status](https://img.shields.io/badge/Status-Core%20Complete-brightgreen)

A robust payment processing microservice built with Spring Boot, implementing a saga-style payment flow, idempotent payment handling, Redis-backed caching, and event-driven communication with Kafka.

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Caching Strategy](#caching-strategy)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [API Documentation](#api-documentation)
- [Project Structure](#project-structure)
- [Future Enhancements](#future-enhancements)
- [Contributing](#contributing)
- [Contact](#contact)

## 🎯 Overview

This is the **Payment Service** component of a larger e-commerce microservices ecosystem. It handles payment initiation and completion as part of a saga (triggered by stock-reservation events), refunds, payment history, and publishes payment events to Kafka for downstream services.

**Part of E-Commerce Microservices Suite:**
- ✅ [Order Service](https://github.com/Akash-boy/Order-service)
- ✅ Payment Service (This repository)
- ✅ [Inventory Service](https://github.com/Akash-boy/Inventory-service)
- ✅ [Analytics Service](https://github.com/Akash-boy/analytical_service.git)

## ✨ Features

### Core Features
- ✅ **Saga-Based Payment Flow** - `PENDING` payment created on `STOCK_RESERVED`, gateway called and finalized on `STOCK_CONFIRMED`
- ✅ **Idempotent Payment Processing** - Prevents duplicate payments using idempotency keys
- ✅ **Payment Status Tracking** - PENDING / SUCCESS / FAILED / REFUNDED
- ✅ **Refund Management** - Refunds with reason tracking, validated against the payment gateway response
- ✅ **Payment History** - Paginated payment history per user
- ✅ **Event-Driven Architecture** - Kafka integration (`PaymentInitiated`, `PaymentCompleted`)
- ✅ **Redis Caching** - Payment lookups cached by both order ID and payment ID

### Technical Features
- ✅ **Comprehensive Validation** - Bean validation with custom business rules
- ✅ **Global Exception Handling** - Centralized error management
- ✅ **Transaction Management** - ACID compliance for payment operations
- ✅ **Logging & Monitoring** - Detailed logging with SLF4J
- ✅ **RESTful API Design** - Clean, intuitive API endpoints
- ✅ **Database Indexing** - Indexes on `orderId`, `userId`, `idempotencyKey`, `status`

## 🏗️ Architecture
```
┌─────────────┐      ┌──────────────────┐      ┌─────────────┐
│   Client    │─────▶│ Payment Service  │─────▶│   MySQL     │
└─────────────┘      └──────────────────┘      └─────────────┘
                              │      │
                    publishes │      │ read-through
                       events │      ▼ cache
                              │  ┌─────────┐
                              │  │  Redis  │
                              │  └─────────┘
                              ▼
                     ┌──────────────────┐
                     │  Apache Kafka    │
                     └──────────────────┘
                              │
                              │ consumed by
                              ▼
                     ┌──────────────────┐
                     │ Analytics Service│
                     │  Order Service   │
                     └──────────────────┘
```

### Payment Saga Flow
```
1. Inventory Service reserves stock → publishes STOCK_RESERVED
2. Payment Service consumes it → creates PENDING payment, publishes PAYMENT_INITIATED
3. Inventory Service confirms stock → publishes STOCK_CONFIRMED
4. Payment Service consumes it → calls Payment Gateway
5. On success: status → SUCCESS, transactionId + gatewayReference saved
   On failure: status → FAILED, failureReason saved
6. Publishes PAYMENT_COMPLETED — Order Service marks the order complete/failed
```

## 🔴 Caching Strategy

Payment Service uses Spring's `@Cacheable` backed by Redis, with **two separate caches** rather than one shared cache:

| Cache | Method | Key |
|---|---|---|
| `paymentsByOrderId` | `getPaymentByOrderId(orderId)` | `orderId` |
| `paymentsById` | `getPaymentById(paymentId)` | `paymentId` |

**Why two caches, not one:** `orderId` and `paymentId` are separate ID spaces that both start counting from 1 — using a single shared cache keyed by whichever ID happened to be passed in would let `getPaymentById(5)` return the payment for **order** 5 instead, silently, with no error. Keeping them in separate cache namespaces makes that collision impossible.

**Eviction — annotation where possible, programmatic where not:**
- `refundPayment(paymentId, amount)` evicts `paymentsById` by `paymentId` directly, but has to evict `paymentsByOrderId` **programmatically** via an injected `CacheManager`, since `orderId` isn't a parameter of that method — it's only known after fetching the `Payment` entity.
- `completePayment(orderId, reservationId)` is the mirror case: evicts `paymentsByOrderId` by `orderId` directly, and evicts `paymentsById` programmatically once the payment's own ID is known.

**A build note worth mentioning:** named SpEL cache keys (`key = "#paymentId"`) failed at runtime here with *"Null key returned... ensure the compiler uses the '-parameters' flag"* — this project's Gradle build wasn't compiling with that flag. Rather than fight the build config, the keys use positional SpEL references (`#p0`) instead, which don't depend on the compiler preserving parameter names.

**TTL:** 10 minutes, same as the rest of the suite.

## 🛠️ Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 | Programming Language |
| Spring Boot | 4.0.1 | Application Framework |
| Spring Data JPA | 4.x | Database Access |
| Hibernate | 7.x | ORM Framework |
| MySQL | 8.3 | Relational Database |
| Apache Kafka | 7.4.4 (KRaft mode) | Message Broker |
| Redis | 7.2 | Caching Layer |
| Lombok | 1.18.x | Boilerplate Reduction |
| Gradle | 8.x | Build Tool |
| SLF4J + Logback | Latest | Logging |

## 🚀 Getting Started

### Prerequisites
```bash
# Required
- Java 21 or higher
- Gradle 8.x (or use the included ./gradlew wrapper)
- Docker Desktop (for MySQL, Kafka, Redis)

# Recommended
- IntelliJ IDEA
- Postman (for API testing)
```

### Installation

#### 1. Clone the Repository
```bash
git clone https://github.com/Akash-boy/Payment-service.git
cd Payment-service
```

#### 2. Start Infrastructure
```bash
docker compose up -d
```

#### 3. Configure Database

`src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/ecommerce_db
spring.datasource.username=root
spring.datasource.password=password

spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.cache.type=redis
spring.cache.redis.time-to-live=600000
```

#### 4. Build & Run
```bash
./gradlew clean build
./gradlew bootRun
```

The service starts on `http://localhost:9090`

### Quick Test
```bash
# Health check
curl http://localhost:9090/actuator/health

# Fetch a payment by ID twice — second call served from Redis
curl http://localhost:9090/api/v1/payments/1
curl http://localhost:9090/api/v1/payments/1

# Confirm in Redis
docker exec -it redis redis-cli KEYS "payments*"

# Refund a payment
curl -X POST http://localhost:9090/api/v1/payments/1/refund \
  -H "Content-Type: application/json" \
  -d '{ "amount": 100.00, "reason": "Customer requested refund" }'
```

## 📚 API Documentation

### Base URL
```
http://localhost:9090/api/v1/payments
```

### Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/` | Manually trigger payment initiation for an order |
| GET | `/order/{orderId}` | Get payment by order ID *(cached)* |
| GET | `/{paymentId}` | Get payment by payment ID *(cached)* |
| GET | `/user/{userId}` | Get paginated payment history for a user |
| POST | `/{paymentId}/refund` | Refund a payment *(evicts both caches)* |

#### Refund a Payment
```http
POST /api/v1/payments/1/refund
Content-Type: application/json

{
  "amount": 100.00,
  "reason": "Customer requested refund"
}
```

**Response (200 OK):**
```json
{
  "id": 1,
  "orderId": 123,
  "status": "REFUNDED",
  "amount": 250.00,
  "transactionId": "txn_abc123"
}
```

## 📁 Project Structure
```
payment-service/
├── src/
│   ├── main/
│   │   ├── java/com/example/
│   │   │   ├── controller/
│   │   │   │   └── PaymentController.java
│   │   │   ├── service/
│   │   │   │   └── PaymentService.java
│   │   │   ├── repository/
│   │   │   │   └── PaymentRepository.java
│   │   │   ├── entities/
│   │   │   │   ├── Payment.java
│   │   │   │   └── PaymentStatus.java
│   │   │   ├── dto/
│   │   │   │   ├── PaymentRequest.java
│   │   │   │   └── RefundRequest.java
│   │   │   ├── config/
│   │   │   │   └── RedisConfig.java
│   │   │   ├── gateway/
│   │   │   │   ├── PaymentGateway.java
│   │   │   │   └── PaymentGatewayResponse.java
│   │   │   ├── client/
│   │   │   │   ├── OrderServiceClient.java
│   │   │   │   └── InventoryServiceClient.java
│   │   │   ├── exception/
│   │   │   └── kafka/
│   │   │       └── PaymentEventProducer.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
├── .gitignore
├── build.gradle
├── README.md
└── LICENSE
```

## 🎯 Future Enhancements

- [ ] Integration with a real payment gateway (Stripe, Razorpay) — currently uses a mock gateway
- [ ] Retry mechanism for failed payments with exponential backoff
- [ ] Circuit breaker pattern using Resilience4j
- [ ] Webhook endpoints for payment gateway callbacks
- [ ] Docker containerization of the app itself
- [ ] Deployment to AWS free tier (EC2 + RDS)
- [ ] OpenAPI/Swagger documentation

### Learning Goals
This project is built to learn and demonstrate:
- ✅ Microservices architecture
- ✅ Saga pattern for distributed transactions
- ✅ Event-driven design with Kafka
- ✅ Redis caching with multiple keyspaces + programmatic eviction via `CacheManager`
- ✅ Idempotency and error handling strategies
- 🚧 Container orchestration
- 🚧 CI/CD pipelines

## 🤝 Contributing

Contributions are welcome! This is a learning project.

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

**Akash**
- GitHub: [@Akash-boy](https://github.com/Akash-boy)

## 📊 Project Status

**Current Status:** ✅ Core Features + Redis Caching Complete

---

⭐ If you found this project helpful, please give it a star!
