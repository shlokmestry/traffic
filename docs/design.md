# Design — Redis + Lua Token Bucket Rate Limiter

## Overview
This service implements a distributed Token Bucket algorithm backed by Redis.  
Each request executes one Redis Lua script that refills tokens and optionally consumes them atomically.

## Components
- Rate Limiter Service (Spring Boot): orchestrates requests and returns decisions.
- Redis: stores per-identity bucket state.
- Lua script: performs atomic refill + consume and returns decision data.
- Observability stack: Micrometer → Prometheus → Grafana dashboards.

## Request flow
1. Client (or gateway) calls Rate Limiter with `{ruleId, identity}` (and rule parameters).
2. Service executes Lua script against Redis key `tb:{ruleId}:{identity}`.
3. Script returns:
   - allowed (0/1)
   - retryAfterMs
   - remaining (floor(tokens))
4. Service converts result into response; on errors, applies fail-closed fallback.

## Redis data model
Key:
- `tb:{ruleId}:{identity}`

Hash fields:
- `tokens` (number)
- `lastrefillms` (ms timestamp)

TTL:
- PEXPIRE is set so the key self-deletes when idle.

## Lua script logic (summary)
Inputs:
- nowMs
- capacity
- refillTokensPerSecond
- cost
- ttlMs

Algorithm:
- Load `tokens` and `lastrefillms` (default tokens=capacity, lastrefillms=nowMs).
- Refill: `tokens = min(capacity, tokens + elapsedSeconds * refillTokensPerSecond)`.
- If `tokens >= cost`: allow and deduct; else deny.
- If denied, compute `retryAfterMs` from deficit and refill rate.
- Persist state + set TTL.
- Return `[allowed, retryAfterMs, floor(remaining)]`.

Atomicity:
Redis runs the Lua script atomically, preventing race conditions across multiple instances.

## Failure handling (fail-closed)
If Redis is unavailable, script execution fails, or the response is malformed:
- deny the request
- return a conservative retryAfterMs (e.g., 1000ms)
- increment `ratelimit.failclosed.total` with reason tag:
  - rediserror
  - badresponse
  - badtypes

This protects downstream services during partial failures.

## Observability
Dashboards:
- Service Up (Prometheus `up` metric)
- 5xx rate (5m)
- Fail-closed events (5m)

Metrics of interest:
- `ratelimit.failclosed.total{reason=...}` for failure-mode visibility.

## Trade-offs
- Redis centralizes state: simple and consistent, but creates dependency on Redis availability.
- Token Bucket is burst-friendly vs fixed window.
- Fail-closed protects backends but may reduce availability during Redis incidents.
