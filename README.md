# Configuration Change Tracker Service

A senior-architect designed service for tracking configuration changes across multiple rule types.

## Features
- **Java 21 & Spring Boot 3.x**: Leverages modern Java features like `sealed interfaces` and `records`.
- **Domain-Driven Design**: Focused on making illegal states unrepresentable.
- **API-First**: Standard RESTful endpoints for CRUD-like change operations.
- **In-Memory Persistence**: Uses H2 and JPA for history and current state tracking.
- **Kafka Integration**: Publishes change events to the `config-changes` topic.
- **Monitoring**: Notifies an external service for critical changes (e.g., deletions).
- **Observability**: Health and metrics endpoints via Spring Boot Actuator.

## Domain Model
The core domain uses a sealed interface `RulePayload` with specific implementations for:
- `CreditLimitPayload`
- `ApprovalPolicyPayload`

This ensures that only valid payloads can be associated with specific rule types.

## API Endpoints
- `POST /api/v1/changes`: Create a new configuration change.
- `GET /api/v1/changes/{id}`: Retrieve a specific change by its UUID.
- `GET /api/v1/changes?type={type}`: List changes by rule type.
- `GET /api/v1/changes?from={start}&to={end}`: List changes within a time range.

## Running the application
```bash
./mvnw spring-boot:run
```

## Testing
Unit tests cover the domain model consistency:
```bash
./mvnw test
```

## Future Roadmap
- **OAuth2 Integration**: The `actor` field is already present in the domain model to support auditing.
- **React SPA**: RFC 7807 Problem Details and CORS-ready controllers.
