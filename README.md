# Limited Goods

Limited Goods is a commerce backend project focused on limited-stock product sales.
The project is designed around real production concerns such as overselling, duplicate
payments, order expiration, inventory reservation, outbox event publishing, monitoring,
and load testing.

This is not a simple product/order CRUD application. The main goal is to show how a
backend service can be prepared for reliable operation.

## Goals

- Prevent overselling during high-concurrency limited-stock sales
- Reserve inventory with Redis before final payment completion
- Protect payment requests with idempotency keys
- Separate payment approval from final order confirmation
- Publish order events through a transactional outbox pattern
- Prevent duplicate event consumption
- Split Docker Compose files by runtime purpose
- Observe the service with Prometheus and Grafana
- Validate behavior with k6 load tests
- Prepare the project for CI/CD and cloud deployment

## Tech Stack

### Backend

- Java 17
- Spring Boot 3.5
- Spring Security / JWT
- Spring Data JPA / JDBC
- PostgreSQL
- Redis / Redisson
- Kafka
- Prometheus / Grafana
- JUnit

### Frontend

- React 19
- Vite
- lucide-react

### DevOps

- Docker
- Docker Compose
- k6
- GitHub Actions
- Cloud deployment planned

## Main Features

- User signup and login
- Product registration, update, and listing
- Redis-based inventory reservation
- Order creation
- Order expiration and inventory recovery
- Payment request and failure handling
- Payment idempotency
- Order cancellation and refund
- Admin order search
- Kafka outbox event publishing
- Prometheus metrics
- k6 load testing

## System Overview

```text
React Frontend
  -> Spring Boot API
      -> PostgreSQL
      -> Redis
      -> Kafka
      -> Prometheus / Grafana
```

## Docker Compose Layout

The Docker Compose setup is split by runtime purpose. This keeps the default local
environment lightweight while still allowing a full demo stack when needed.

| File | Purpose | Services |
| --- | --- | --- |
| `docker-compose.yml` | Lightweight local development | PostgreSQL, Redis |
| `docker-compose.kafka.yml` | Event/outbox demo | Kafka |
| `docker-compose.app.yml` | Backend container runtime | Spring Boot app |
| `docker-compose.observability.yml` | Monitoring demo | Prometheus, Grafana |

See [Docker Compose Guide](./docs/docker-compose-guide.md) for more details.

### 1. Local Development Infrastructure

Use this when running Spring Boot from the IDE or Gradle on the host machine.

```powershell
docker compose up -d
```

```powershell
.\gradlew.bat bootRun
```

Local endpoints:

- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`
- Backend API: `localhost:8080`

### 2. Backend Container Demo

Use this when running the backend itself as a Docker container.

```powershell
docker compose `
  -f docker-compose.yml `
  -f docker-compose.kafka.yml `
  -f docker-compose.app.yml `
  up -d --build
```

### 3. Full Demo With Monitoring

Use this for portfolio demos, monitoring screenshots, and load-test evidence.

```powershell
docker compose `
  -f docker-compose.yml `
  -f docker-compose.kafka.yml `
  -f docker-compose.app.yml `
  -f docker-compose.observability.yml `
  up -d --build
```

Demo endpoints:

- Backend API: `http://localhost:8080`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`

## Environment Variables

Copy `.env.example` to `.env` before customizing runtime values.

```powershell
Copy-Item .env.example .env
```

Important variables:

- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `JWT_SECRET`
- `SPRING_PROFILES_ACTIVE`
- `APP_PORT`
- `REDIS_PORT`
- `KAFKA_PORT`

For production-like environments, secrets such as database passwords, JWT secrets,
and Grafana passwords should be injected through environment variables or a secret
manager instead of being stored in source code.

## Tests

```powershell
.\gradlew.bat test
```

Main test targets:

- Redis inventory reservation
- Overselling prevention under concurrent order requests
- Order creation and payment flow
- Duplicate payment request prevention
- Order expiration and inventory recovery
- Refund and cancellation state transitions
- Outbox event publishing and duplicate processing prevention

## CI/CD

GitHub Actions runs backend tests automatically on every push and on pull requests
targeting `main` or `develop`.

Workflow file:

- `.github/workflows/backend-ci.yml`

The backend CI pipeline:

1. Checks out the repository
2. Sets up Java 17
3. Starts PostgreSQL, Redis, and Kafka with `docker-compose.ci.yml`
4. Waits for required services to become available
5. Runs `./gradlew test --no-daemon`
6. Uploads Gradle test reports as an artifact
7. Stops and removes CI dependency containers

CI dependency file:

- `docker-compose.ci.yml`

## Load Testing

The k6 script is located at `k6/k6-order-test.js`.

Load-test goals:

- Verify that successful orders never exceed available stock
- Check Redis reservation and database inventory consistency
- Measure order API latency and failure rate under concurrent traffic

Example scenario:

```text
100+ users request orders at the same time for a product with stock 10.
Only up to 10 orders should succeed.
The rest should fail because of insufficient stock.
```

## Monitoring

The project uses Spring Boot Actuator, Prometheus, and Grafana.

Metrics to observe:

- HTTP request count
- HTTP response latency
- Order creation success/failure count
- Payment success/failure count
- Expired order count
- JVM memory
- Application health check

The Docker-specific Prometheus config is located at `monitoring/prometheus-docker.yml`.

## DevOps Portfolio Direction

This project is being expanded from a backend project into an operation-ready service.

Planned DevOps work:

- GitHub Actions test/build pipeline
- Docker image build pipeline
- Frontend static deployment
- Low-cost backend cloud deployment
- External PostgreSQL and Redis integration
- Environment-specific configuration
- Incident scenario documentation
- k6 load-test result documentation
- Grafana dashboard screenshots and metric explanations

## Incident Scenarios To Document

- Redis outage and its impact on order creation
- Database connection failure and health check behavior
- Kafka publish failure and outbox retry behavior
- Failure after payment approval and recovery strategy
- Deployment failure and rollback strategy
- Traffic spike and bottleneck analysis

## Cloud Deployment Strategy

To reduce cost, the first deployment target should keep only the critical runtime
path always-on.

Initial low-cost deployment plan:

- Frontend: Cloudflare Pages or S3/CloudFront
- Backend: Render, Railway, Fly.io, or EC2
- PostgreSQL: Neon or RDS
- Redis: Upstash or Docker Redis on a small server

Kafka, Prometheus, and Grafana can stay in the local/demo environment at first.
For a larger production setup, these can be replaced or expanded with MSK, SQS,
EventBridge, CloudWatch, or managed Grafana.

## Portfolio Summary

Limited Goods is a limited-stock commerce backend project containerized with Docker
Compose. It validates production-style concerns such as Redis inventory reservation,
payment idempotency, order expiration, Kafka outbox processing, Prometheus/Grafana
monitoring, and k6 load testing.
