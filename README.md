# 🛒 E-Commerce Microservices - Payment Service

A robust payment processing microservice built with Spring Boot, featuring idempotent payment handling, event-driven architecture with Kafka, and comprehensive error management.

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)
![Kafka](https://img.shields.io/badge/Apache%20Kafka-Enabled-black)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)
![Status](https://img.shields.io/badge/Status-In%20Development-yellow)

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [API Documentation](#api-documentation)
- [Project Structure](#project-structure)
- [Future Enhancements](#future-enhancements)
- [Contributing](#contributing)
- [Contact](#contact)

## 🎯 Overview

This is the **Payment Service** component of a larger e-commerce microservices ecosystem. It handles payment processing, refunds, payment history, and publishes payment events to Kafka for downstream services.

**Part of E-Commerce Microservices Suite:**
- ✅ Order Service
- ✅ Payment Service (This repository)
- 🚧 Inventory Service (Coming soon)
- 🚧 Analytics Service (Coming soon)

## ✨ Features

### Core Features
- ✅ **Idempotent Payment Processing** - Prevents duplicate payments using idempotency keys
- ✅ **Multiple Payment Methods** - Credit Card, Debit Card, UPI, Net Banking, Wallet
- ✅ **Payment Status Tracking** - Real-time payment status updates
- ✅ **Refund Management** - Full and partial refunds with validation
- ✅ **Payment History** - Paginated payment history per user
- ✅ **Event-Driven Architecture** - Kafka integration for asynchronous communication

### Technical Features
- ✅ **Comprehensive Validation** - Bean validation with custom business rules
- ✅ **Global Exception Handling** - Centralized error management
- ✅ **Transaction Management** - ACID compliance for payment operations
- ✅ **Logging & Monitoring** - Detailed logging with SLF4J
- ✅ **RESTful API Design** - Clean, intuitive API endpoints
- ✅ **Database Indexing** - Optimized queries for performance

## 🏗️ Architecture
```
┌─────────────┐      ┌──────────────────┐      ┌─────────────┐
│   Client    │─────▶│ Payment Service  │─────▶│   MySQL     │
└─────────────┘      └──────────────────┘      └─────────────┘
                              │
                              │ publishes events
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

### Payment Processing Flow
```
1. Client sends payment request with idempotency key
2. Service checks for duplicate using idempotency key
3. If duplicate → Return existing payment (409 Conflict)
4. If new → Create payment record (Status: PENDING)
5. Process through Payment Gateway
6. Update payment status (SUCCESS/FAILED)
7. Publish event to Kafka
8. Return response to client
```

## 🛠️ Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 17 | Programming Language |
| Spring Boot | 3.x | Application Framework |
| Spring Data JPA | 3.x | Database Access |
| Hibernate | 6.x | ORM Framework |
| MySQL | 8.0 | Relational Database |
| Apache Kafka | 3.x | Message Broker |
| Lombok | 1.18.x | Boilerplate Reduction |
| Maven | 3.8+ | Build Tool |
| SLF4J + Logback | Latest | Logging |

## 🚀 Getting Started

### Prerequisites
```bash
# Required
- Java 17 or higher
- Maven 3.8+
- MySQL 8.0+
- Apache Kafka 3.x (or Docker)

# Recommended
- IntelliJ IDEA / Eclipse
- Postman (for API testing)
- Docker Desktop
```

### Installation

#### 1. Clone the Repository
```bash
git clone https://github.com/yourusername/payment-service.git
cd payment-service
```

#### 2. Configure Database

Create MySQL database:
```sql
CREATE DATABASE payment_service;
```

Update `src/main/resources/application.properties`:
```properties
# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/payment_service
spring.datasource.username=your_username
spring.datasource.password=your_password

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# Kafka Configuration
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
```

#### 3. Start Kafka (using Docker)
```bash
# Start Zookeeper
docker run -d --name zookeeper -p 2181:2181 zookeeper

# Start Kafka
docker run -d --name kafka -p 9092:9092 \
  --link zookeeper \
  -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  confluentinc/cp-kafka
```

#### 4. Build & Run
```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run

# Or run the JAR
java -jar target/payment-service-0.0.1-SNAPSHOT.jar
```

The service will start on `http://localhost:8080`

### Quick Test
```bash
# Health check
curl http://localhost:8080/actuator/health

# Create a test payment
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 1,
    "userId": 1,
    "amount": 100.50,
    "paymentMethod": "CREDIT_CARD",
    "idempotencyKey": "unique-key-12345"
  }'
```

## 📚 API Documentation

### Base URL
```
http://localhost:8080/api/v1/payments
```

### Endpoints

#### 1. Create Payment
```http
POST /api/v1/payments
Content-Type: application/json

{
  "orderId": 123,
  "userId": 456,
  "amount": 250.00,
  "paymentMethod": "CREDIT_CARD",
  "idempotencyKey": "unique-uuid-here"
}
```

**Response (201 Created):**
```json
{
  "id": 1,
  "orderId": 123,
  "userId": 456,
  "amount": 250.00,
  "paymentMethod": "CREDIT_CARD",
  "status": "SUCCESS",
  "transactionId": "txn_abc123",
  "createdAt": "2026-01-28T10:30:00"
}
```

#### 2. Get Payment by Order ID
```http
GET /api/v1/payments/order/{orderId}
```

#### 3. Get User Payment History (Paginated)
```http
GET /api/v1/payments/user/{userId}?page=0&size=10&sortBy=createdAt&sortDirection=DESC
```

**Response:**
```json
{
  "content": [ /* array of payments */ ],
  "totalPages": 3,
  "totalElements": 25,
  "number": 0,
  "size": 10
}
```

#### 4. Refund Payment
```http
POST /api/v1/payments/{paymentId}/refund
Content-Type: application/json

{
  "amount": 100.00,
  "reason": "Customer request"
}
```

### Error Responses
```json
{
  "timestamp": "2026-01-28T10:30:45",
  "status": 400,
  "error": "Validation Failed",
  "message": "Invalid input parameters",
  "details": {
    "amount": "Amount must be greater than 0"
  }
}
```

## 📁 Project Structure
```
payment-service/
├── src/
│   ├── main/
│   │   ├── java/com/example/
│   │   │   ├── controller/          # REST Controllers
│   │   │   │   └── PaymentController.java
│   │   │   ├── service/             # Business Logic
│   │   │   │   └── PaymentService.java
│   │   │   ├── repository/          # Data Access Layer
│   │   │   │   └── PaymentRepository.java
│   │   │   ├── entities/            # JPA Entities
│   │   │   │   ├── Payment.java
│   │   │   │   └── PaymentStatus.java
│   │   │   ├── dto/                 # Data Transfer Objects
│   │   │   │   ├── PaymentRequest.java
│   │   │   │   └── RefundRequest.java
│   │   │   ├── exception/           # Custom Exceptions
│   │   │   │   ├── PaymentException.java
│   │   │   │   ├── DuplicatePaymentException.java
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── ErrorResponse.java
│   │   │   ├── gateway/             # Payment Gateway Integration
│   │   │   │   ├── PaymentGateway.java
│   │   │   │   ├── PaymentGatewayResponse.java
│   │   │   │   └── impl/
│   │   │   │       └── MockPaymentGateway.java
│   │   │   └── eventProducer/       # Kafka Producers
│   │   │       └── PaymentEventProducer.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── application-dev.properties
│   └── test/                        # Unit & Integration Tests
├── .gitignore
├── pom.xml
├── README.md
├── LICENSE
└── CONTRIBUTING.md
```

## 🎯 Future Enhancements

### Planned Features
- [ ] Integration with real payment gateways (Stripe, Razorpay)
- [ ] Retry mechanism for failed payments with exponential backoff
- [ ] Circuit breaker pattern using Resilience4j
- [ ] Payment analytics dashboard
- [ ] Webhook endpoints for payment gateway callbacks
- [ ] Multi-currency support
- [ ] Payment fraud detection
- [ ] Docker containerization
- [ ] Kubernetes deployment manifests
- [ ] Comprehensive integration tests
- [ ] API rate limiting
- [ ] OpenAPI/Swagger documentation

### Learning Goals
This project is built to learn and demonstrate:
- ✅ Microservices architecture
- ✅ Event-driven design with Kafka
- ✅ RESTful API best practices
- ✅ Database optimization
- ✅ Error handling strategies
- 🚧 Container orchestration
- 🚧 CI/CD pipelines
- 🚧 Distributed tracing

## 🤝 Contributing

Contributions are welcome! This is a learning project, and I'm open to suggestions and improvements.

### How to Contribute

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Code Style
- Follow Java naming conventions
- Add comments for complex logic
- Write unit tests for new features
- Update README if adding new features

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

**Animesh**
- GitHub: [Akash-boy](https://github.com/Akash-boy)
- Email: akashzaminder@gmail.com

## 🙏 Acknowledgments

- Spring Boot documentation
- Kafka documentation
- Stack Overflow community

## 📊 Project Status

**Current Status:** 🚧 In Active Development

**Completion:** 
- Order Service: ✅ Complete
- Payment Service: ✅ 80% Complete
- Inventory Service: 🚧 In Progress
- Analytics Service: 📋 Planned

---

⭐ If you found this project helpful, please give it a star!

💬 Questions? Feel free to open an issue or reach out!