traffic-service 🚦
A centralized distributed rate limiting service for shared APIs
traffic-service is a centralized service that determines whether a request should be allowed or blocked — consistently and safely — across all your services.
It enforces rate limits using a Redis-backed Token Bucket with atomic Lua execution, ensuring correct behavior even when multiple app instances are running.
You integrate it the same way you call any internal service:
check the rate limit before processing the request.

___________________________________________________________________________________________________

Why does this exist?
Rate limiting implemented independently in each service usually leads to:
inconsistent enforcement across teams
race conditions in distributed systems
poor burst handling
duplicated logic
limited observability when failures happen
traffic-service centralizes rate limiting so enforcement is predictable, burst-friendly, and operationally visible.

___________________________________________________________________________________________________

When should I use this?
Use traffic-service when:
You have shared APIs used by multiple teams or customers
You run multiple service instances and need correct enforcement
You want burst support (Token Bucket), not fixed windows
You need safe behavior during Redis failures
You want metrics you can alert on

Features
Token Bucket algorithm (burst-friendly)
Atomic updates using Redis + Lua
Consistent enforcement across instances
Fail-closed behavior to protect downstream services
Prometheus metrics for observability
Grafana-ready dashboards
Low-latency: one Redis call per request

___________________________________________________________________________________________________

Quick Start:
Start Redis and the service:
docker compose up --build

Check a rate limit:
curl -X POST http://localhost:8080/ratelimit/check \
  -H "Content-Type: application/json" \
  -d '{
    "ruleId": "api-read",
    "identity": "user_123"
  }'
  
Response:
{
  "allowed": true,
  "remaining": 42,
  "retryAfterMs": 0
}

___________________________________________________________________________________________________

How it works
Each request sends { ruleId, identity } to traffic-service
A Redis Lua script:
refills tokens based on elapsed time
consumes a token if available
executes atomically
The result is returned to the caller
No race conditions.
No cross-node coordination.

___________________________________________________________________________________________________

Usage pattern
Typical gateway flow:
Call traffic-service
If allowed = true → forward request
If allowed = false → return HTTP 429
Example blocked response:
{
  "allowed": false,
  "remaining": 0,
  "retryAfterMs": 1200
}
