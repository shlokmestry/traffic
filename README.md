# Distributed Rate Limiting Service 🚦

A centralized distributed rate limiting service that protects shared APIs from abuse, traffic spikes, and accidental overload.

You integrate it the same way you normally call your backend — just add a rate-limit check before the request is processed.

---

## When should I use this?

Use **distributed-rate-limiter** when:

- You have shared APIs used by multiple teams/customers
- You want consistent rate limiting across many backend services
- You need burst support (not just fixed windows)
- You run multiple app instances and still need correct enforcement
- You want clear operational signals (metrics + dashboards)

This service helps you enforce fair usage while keeping latency low.

---

## Features

- Token Bucket algorithm (burst-friendly)
- Distributed-safe atomic updates via Redis + Lua script
- Fail-closed safety mode when Redis is unhealthy (protects downstream services)
- Metrics for failure modes (`ratelimit.failclosed.total{reason=...}`)
- Grafana-ready dashboard signals (Service Up, 5xx rate, fail-closed events)

---

## How it works

Each identity (API key / user / IP) has a token bucket stored in Redis.  
On every request, the service runs a Redis Lua script that refills tokens based on elapsed time and consumes tokens if available — all **atomically**.

The script returns:
- `allowed` (0/1)
- `retryAfterMs`
- `remaining`

If Redis is unavailable or the script response is invalid, the service **fails closed** and increments `ratelimit.failclosed.total` with a reason tag.

---

## Installation

### Prerequisites
- Redis running locally on `localhost:6379`
- Java + your build tool (Maven/Gradle)

### Config
`application.yaml` uses Redis at `localhost:6379` by default.

### Run
Use your normal Spring Boot run command (Maven or Gradle).

---

## Usage

> Update the endpoint path below to match your controller (send me your endpoint and I’ll fill this in exactly).

Example flow:
1) Call the rate limiter with `{ruleId, identity}`.
2) If allowed: continue to backend.
3) If blocked: return HTTP 429 + retry-after info.

---

## Metrics & dashboards

PromQL examples used in Grafana:

```promql
# 5xx rate (5m)
sum(rate(http_server_requests_seconds_count{job="traffic-service", outcome="SERVER_ERROR"}[5m])) or on() vector(0)

# Fail-closed events (5m)
sum(increase(ratelimit_failclosed_total{job="traffic-service"}[5m])) or on() vector(0)
