# traffic# Distributed Rate Limiting Service for Shared APIs

A centralized distributed rate limiting service designed to protect shared APIs from abuse, traffic spikes, and accidental overload.  
It enforces configurable request limits per user/IP/API key with low latency and predictable failure behavior.

## What this project solves
When multiple teams/services share APIs, rate limiting often gets duplicated (and implemented differently) in each backend or gateway.  
This project provides one place to define and enforce limits consistently.

## Key features
- Token Bucket rate limiting (burst-friendly).
- Distributed-safe enforcement using Redis + Lua (atomic check-and-consume).
- Fail-closed behavior for Redis/script failures (protects upstream services).
- Prometheus/Micrometer metrics + Grafana dashboards.

## High-level architecture
Typical request flow:
Client → API Gateway (or edge) → Rate Limiter Service → Backend

State:
Bucket state is stored in Redis per `{ruleId, identity}` and updated atomically by a Lua script.

## Algorithm (Token Bucket)
Per identity:
- `capacity`: max tokens (burst)
- `refillTokensPerSecond`: steady refill rate
- `cost`: tokens consumed per request

On each request:
1. Refill tokens based on time elapsed since last refill.
2. If tokens ≥ cost: allow and decrement.
3. Else: deny and return a computed `retryAfterMs`.

Atomicity:
All steps happen inside one Redis Lua script to avoid race conditions across multiple app instances.

## Observability
Example Grafana panels:
- Service Up: `up{job="traffic-service", instance="host.docker.internal:8082"}`
- 5xx rate (5m):
  `sum(rate(http_server_requests_seconds_count{job="traffic-service", outcome="SERVER_ERROR"}[5m])) or on() vector(0)`
- Fail-closed events (5m):
  `sum(increase(ratelimit_failclosed_total{job="traffic-service"}[5m])) or on() vector(0)`

Fail-closed counters:
- `ratelimit.failclosed.total{reason="rediserror"}`
- `ratelimit.failclosed.total{reason="badresponse"}`
- `ratelimit.failclosed.total{reason="badtypes"}`

## Local setup (quick)
Prereqs:
- Redis running locally (`localhost:6379`)
- Java runtime

Config (see `application.yaml`):
- `spring.data.redis.host=localhost`
- `spring.data.redis.port=6379`
- `server.port=8081`

Run:
- Start the Spring Boot app (your usual Gradle/Maven command).
- Hit your rate-limit endpoint(s) or integrate via gateway.

## Docs
- Requirements: `docs/requirements.md`
- Design: `docs/design.md`

## Roadmap (nice-to-have)
- Rule CRUD API + storage (create/update/list rules).
- Per-endpoint limits, plan tiers (Free/Pro), dynamic rule updates.
- Load testing and SLO targets.
