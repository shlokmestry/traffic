# Requirements — Distributed Rate Limiting Service

## Problem statement
Provide a centralized rate limiting service to protect shared APIs from abuse and spikes, with consistent enforcement across multiple service instances.

## Primary use cases
- Public APIs exposed to the internet.
- Authentication endpoints (login/OTP).
- Per-customer or per-API-key traffic control.
- Prevent accidental overload during retries/buggy clients.

## Functional requirements
1. Identity-based limiting:
   - Support per API key / user ID / IP address (identity is a string key).
2. Configurable token bucket rules:
   - capacity (burst size)
   - refill rate (tokens per second)
   - cost per request
   - TTL for idle keys
3. Decision response:
   - allowed (true/false)
   - retryAfterMs (when blocked)
   - remaining tokens (integer)
4. Distributed correctness:
   - multiple application instances must be safe; no race conditions.
5. Key lifecycle management:
   - expired/idle identities should be removed (TTL) to avoid unbounded Redis growth.

## Non-functional requirements
- Low latency: one Redis operation per decision (Lua script).
- Reliability: explicit behavior when Redis is down (fail-open or fail-closed).
- Observability:
  - metrics for errors / fail-closed events
  - dashboards for service up, error rate, fail-closed events
- Operability:
  - easy local setup
  - clear runbook-style troubleshooting signals

## Constraints and assumptions
- Redis is the shared state store.
- Lua script execution is atomic in Redis, providing correct read-modify-write.
- In fail-closed mode, requests are denied on Redis/script errors to protect backends.

## Out of scope (for this iteration)
- Multi-region active-active data store.
- Billing and quotas tied to payments.
- Full developer portal for customers.
