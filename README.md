# Config Tracker Service

A high-performance, resilient, and multi-tenant microservice designed to track, audit, and manage configuration changes across complex enterprise environments.

## 1. Project Overview

### Purpose
In large-scale distributed systems, tracking *who* changed *what* configuration and *when* is critical for stability, security, and compliance. The Config Tracker Service provides a centralized "source of truth" for configuration state changes, ensuring every modification is validated, audited, and propagated reliably.

### Business Problem Solved
- **Shadow IT & Hidden Changes**: Prevents undocumented configuration drifts that lead to "it worked yesterday" production outages.
- **Compliance & Auditing**: Provides a tamper-evident audit trail (Immutable History) required for SOC2, GDPR, and financial regulations.
- **Concurrency Conflicts**: Prevents "Lost Updates" where multiple administrators overwrite each other's changes.
- **System Synchronization**: Notifies downstream systems of configuration changes in real-time.

### Key Capabilities
- **Generic Rule Engine**: Supports multiple rule types (Credit Limits, Approval Policies, etc.) using a flexible JSON schema.
- **State Versioning**: Maintains both the current "Active" state and a full historical timeline of changes.
- **Optimistic Concurrency**: Mandatory `oldPayload` verification for updates to ensure state consistency.
- **Critical Change Monitoring**: Automated detection and alerting for high-risk modifications (e.g., lowering security thresholds).

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
- **Security Boundary**: The tenant context is cleared after every request to prevent "context leaking" between execution threads.

### Data Isolation Guarantees
- **Hibernate Filters**: All repository queries are automatically scoped using Spring Data JPA `@Query` annotations that inject the current `TenantContext.getTenantId()`.
- **Primary Keys**: Resource keys (like `customerId` or `policyName`) only need to be unique *within* a tenant.
- **Validation**: The service verifies that a rule exists *for the current tenant* before allowing an `UPDATE` or `DELETE`.

### Usage Example
```http
POST /api/v1/changes
X-Tenant-Id: acme-corp
Content-Type: application/json

{
  "type": "CREDIT_LIMIT",
  "operation": "ADD",
  "actor": "admin-user",
  "payload": {
    "amount": 5000,
    "currency": "USD",
    "customerId": "CUST-99"
  }
}
```

### Scaling & Considerations
- **Scaling**: Since the service is stateless (relying on DB for state), it can scale horizontally (O(n)). 
- **Database Partitioning**: For very large tenant counts, the `tenant_id` column is an ideal candidate for database partitioning/sharding.
- **Limitations**: Currently, cross-tenant reporting is disabled by design to maintain zero-leakage guarantees.

---

## 3. Operational Resiliency

### Circuit Breaker Pattern
Integration with external notification systems is protected by **Resilience4j**. If a downstream system becomes slow or unavailable:
- The circuit opens to prevent resource exhaustion (thread hanging).
- The system provides graceful degradation via a `GlobalExceptionHandler` returning `503 Service Unavailable`.
- **Configuration**:
    - `failureRateThreshold`: 50%
    - `waitDurationInOpenState`: 10s
    - `slidingWindowSize`: 10 calls

### Concurrency Protection
The service implements a strict **Compare-and-Swap (CAS)** pattern for updates. Users must provide the `oldPayload`. If the system detects that the stored state has changed since the user fetched it, the update is rejected with a `409 Conflict`.

---

## 4. Observability & Monitoring

### Metrics (Micrometer/Prometheus)
The service exposes a rich set of metrics via the `/actuator/prometheus` endpoint:
- **`config_tracker_changes_total`**: Counter of all changes, tagged by `operation` and `rule_type`.
- **`config_tracker_critical_changes_total`**: High-priority counter for sensitive modifications.
- **Standard JVM/Spring Metrics**: Latency (Timer), HTTP throughput, and Heap usage.

### Health Checks
- **Liveness**: `/actuator/health/liveness`
- **Readiness**: `/actuator/health/readiness` (Checks DB connectivity).

---

## 6. Running Grafana and Prometheus

### Prerequisites
- Docker
- Docker Compose

### Start the Stack

Run the following command from the project root:

```bash
docker compose up -d
```

## 7. Getting Started

### Prerequisites
- JDK 21
- Maven 3.9+
- Docker (for Prometheus/Grafana stack)

### Quick Start
1. Build the project:
   ```bash
   ./mvnw clean install
   ```
2. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```
3. Run with full observability stack:
   ```bash
   docker-compose up -d
   ```

### Running Tests
- **Unit Tests**: `./mvnw test`
- **Integration Tests**: `./mvnw test-compile failsafe:integration-test`
- **Coverage Report**: `./mvnw jacoco:report` (View at `target/site/jacoco/index.html`)
