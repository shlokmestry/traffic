# Traffic - Distributed Rate Limiting Service for Shared APIs 🚦

A centralized, distributed rate limiting service that protects shared APIs from abuse and traffic spikes, enforcing consistent rules (per user/IP/API key) across multiple application instances.

Built as an independent service, it allows backend teams to apply consistent traffic control rules without embedding rate-limiting logic into each application.

---

## When should I use this?

Use this service when:

- Multiple services share the same downstream API and need consistent throttling rules
- You want centralized rule management instead of app-by-app configuration drift
- You need low-latency enforcement that still behaves safely under partial failures (explicit fail-closed behavior)

---

## Tech stack

- Java 17 (Spring Boot 4.x)
- Redis (Spring Data Redis / `StringRedisTemplate`)
- Token Bucket rate limiting with atomic Redis Lua script execution
- Observability: Micrometer + Prometheus + Grafana
- Containerization: Docker + Docker Compose
- Testing: Spring Boot Test + integration test for fail-closed behavior

---

## Features

- Distributed Token Bucket rate limiting with atomic updates via Redis + Lua
- Central rule management (create/update and fetch rules)
- Two enforcement styles:
  - `/v1/check` for decisioning (allow/deny + remaining + retryAfter)
  - `/v1/enforce` for gateway-friendly behavior (204/429 + RateLimit headers)
- Fail-closed behavior when Redis is unavailable (safe throttling + `Retry-After`)
- Metrics for allowed vs blocked requests and fail-closed reasons (Micrometer → Prometheus)

---

## How it works

Rules are stored centrally in Redis and referenced by `ruleId`.

Each request is evaluated using a Token Bucket stored in Redis and keyed by:
`(ruleId, endpoint, plan, identityKey)`.

Enforcement executes a Redis Lua script to refill tokens and consume request cost in a single atomic operation, returning:
- `allowed`
- `retryAfterMs`
- `remaining`

If Redis (or the limiter path) is unavailable, the service intentionally fails closed to protect downstream systems.

---

## API overview (v1)

### Upsert a rule
`PUT /v1/rules/{ruleId}` stores or updates a rule definition.

### Get a rule
`GET /v1/rules/{ruleId}` returns the current rule configuration.

### Check (decision API)
`POST /v1/check` returns an allow/deny decision plus `retryAfterMs` and remaining tokens.

### Enforce (gateway-friendly)
`POST /v1/enforce` returns:
- `204 No Content` when allowed (with RateLimit headers)
- `429 Too Many Requests` when blocked (with RateLimit headers + `Retry-After`)

---

## Installation

You can run this service using Docker (recommended), or locally via Maven.

### Option A: Run with Docker Compose (recommended)

This repo includes a `docker-compose.yml` that runs:
- Redis
- `traffic-service` (built from this repo)

```bash
docker compose up --build

---

### Option B: Build a Docker image (Dockerfile)

If you have a Dockerfile in the repo root, you can build and run the service directly.

bash
docker build -t traffic-service:local .
docker run --rm -p 8081:8081 \
  -e SPRING_DATA_REDIS_HOST=host.docker.internal \
  -e SPRING_DATA_REDIS_PORT=6379 \
  traffic-service:local
