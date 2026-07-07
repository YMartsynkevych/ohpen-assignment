# Config Tracker Service

A high-performance, resilient, and multi-tenant microservice designed to **manage configuration rules** and **track the complete history of changes** across complex enterprise environments.

---

## 🚀 Quick Usage

The Config Tracker Service can be accessed and managed through multiple interfaces:

### 1. Front-end Application (`configtracker-fe`)
For a graphical user interface to manage rules and view audit trails, use the companion front-end:
- **Repository**: `https://github.com/ohpen/configtracker-fe`
- **Access**: Usually available at `http://localhost:5174` after setup.

### 2. API Tools (Postman / Insomnia / cURL)
You can interact directly with the REST API using tools like Postman. 
- **Important**: All requests **must** include the `X-Tenant-Id` header for isolation.

**Example cURL (Create a Rule):**
```bash
curl -X POST http://localhost:8080/api/v1/changes \
     -H "X-Tenant-Id: acme-corp" \
     -H "Content-Type: application/json" \
     -d '{
           "type": "CREDIT_LIMIT",
           "operation": "ADD",
           "actor": "admin-user",
           "payload": {
             "amount": 5000,
             "currency": "USD",
             "customerId": "CUST-99"
           }
         }'
```

---

## 1. Project Overview

### Purpose
The Config Tracker Service serves as a centralized "source of truth" for configuration state. Its primary goal is to **manage business rules** (like Credit Limits or Approval Policies) while maintaining a **tamper-evident audit trail** of every single modification.

### Business Problem Solved
- **Shadow IT & Hidden Changes**: Prevents undocumented configuration drifts that lead to "it worked yesterday" production outages.
- **Compliance & Auditing**: Provides a permanent history (Who, What, When) required for SOC2, GDPR, and financial regulations.
- **Concurrency Conflicts**: Prevents "Lost Updates" where multiple administrators overwrite each other's changes.
- **System Synchronization**: Notifies downstream systems of configuration changes in real-time.

### Key Capabilities
- **Generic Rule Engine**: Supports multiple rule types using a flexible JSON schema.
- **State Versioning**: Maintains both the current "Active" state and a full historical timeline of changes.
- **Optimistic Concurrency**: Mandatory `oldPayload` verification for updates to ensure state consistency.
- **Critical Change Monitoring**: Automated detection and alerting for high-risk modifications.

### Architecture Overview
The service follows a **Hexagonal (Ports & Adapters)** architecture pattern:
- **Core Domain**: Logic for Rule validation, State transitions, and ConfigChange models using Java 21 `sealed interfaces` and `records`.
- **Application Services**: Orchestration of transactions, persistence, and notifications.
- **Infrastructure Adapters**: 
    - **REST API**: Spring MVC with custom interceptors for Tenant context.
    - **Persistence**: JPA with Hibernate (H2 for local, extensible to PostgreSQL).
    - **Notifications**: Resilient external system integration.
    - **Monitoring**: Micrometer integration for Prometheus/Grafana.

---

## 2. Multi-Tenancy Support

Config Tracker is built from the ground up as a **SaaS-first** application, supporting strict isolation between different organizations (Tenants).

### Implementation Model
The service uses a **Discriminator-based Isolation** model (Shared Database, Separate Rows).

- **Tenant Identification**: Every request must provide a tenant context via the `X-Tenant-Id` HTTP header.
- **Context Management**: A `TenantInterceptor` extracts the ID and stores it in a `ThreadLocal` `TenantContext`.
- **Security Boundary**: The tenant context is cleared after every request to prevent "context leaking".

### Data Isolation Guarantees
- **Hibernate Filters**: All repository queries are automatically scoped using the current `TenantContext`.
- **Primary Keys**: Resource keys (like `customerId`) only need to be unique *within* a tenant.
- **Validation**: The service verifies that a rule exists *for the current tenant* before allowing an `UPDATE` or `DELETE`.

---

## 3. API Reference

All API requests require the `X-Tenant-Id` header.

### Config Changes API
`BASE_URL: /api/v1/changes`

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| **POST** | `/` | **Create Change**: Apply an `ADD`, `UPDATE`, or `DELETE` operation to a rule. |
| **GET** | `/{id}` | **Get Change**: Retrieve a specific audit record by its unique UUID. |
| **GET** | `/?type={TYPE}` | **Filter by Type**: List all changes for a specific rule type (e.g., `CREDIT_LIMIT`). |
| **GET** | `/?from={ISO}&to={ISO}` | **Filter by Time**: List all changes within a specific time range. |

### Observability API
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| **GET** | `/actuator/health` | Service health and readiness status. |
| **GET** | `/actuator/prometheus` | Micrometer metrics for Prometheus scraping. |

---

## 4. Operational Resiliency

### Circuit Breaker Pattern
Integration with external notification systems is protected by **Resilience4j**. If a downstream system becomes slow or unavailable, the circuit opens to prevent resource exhaustion.

### Concurrency Protection
The service implements a strict **Compare-and-Swap (CAS)** pattern for updates. Users must provide the `oldPayload`. If the stored state has changed, the update is rejected with a `409 Conflict`.

---

## 5. Observability & Monitoring

### Metrics (Micrometer/Prometheus)
The service exposes metrics via `/actuator/prometheus`:
- **`config_tracker_changes_total`**: Counter of all changes by operation and rule type.
- **`config_tracker_critical_changes_total`**: High-priority counter for sensitive modifications.

### Health Checks
- **Liveness**: `/actuator/health/liveness`
- **Readiness**: `/actuator/health/readiness` (Checks DB connectivity).

---

## 6. Running Grafana and Prometheus

### Prerequisites
- Docker & Docker Compose

### Start the Stack
Run the following command from the project root:
```bash
docker compose up -d
```

---

## 7. Getting Started

### Prerequisites
- JDK 21
- Maven 3.9+

### Quick Start
1. Build the project:
   ```bash
   ./mvnw clean install
   ```
2. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```

### Running Tests
- **Unit Tests**: `./mvnw test`
- **Integration Tests**: `./mvnw test-compile failsafe:integration-test`
- **Coverage Report**: `./mvnw jacoco:report` (View at `target/site/jacoco/index.html`)
