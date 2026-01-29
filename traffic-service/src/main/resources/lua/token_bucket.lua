-- KEYS[1] = bucket key (hash)
-- ARGV[1] = nowMs
-- ARGV[2] = capacity (integer)
-- ARGV[3] = refillTokensPerSecond (number, can be fractional)
-- ARGV[4] = cost (integer)

local key = KEYS[1]

local now = tonumber(ARGV[1])
local capacity = tonumber(ARGV[2])
local refillPerSec = tonumber(ARGV[3])
local cost = tonumber(ARGV[4])

local tokensStr = redis.call('HGET', key, 'tokens')
local lastRefillStr = redis.call('HGET', key, 'last_refill_ms')

local tokens = tokensStr and tonumber(tokensStr) or capacity
local lastRefill = lastRefillStr and tonumber(lastRefillStr) or now

local elapsedMs = now - lastRefill
if elapsedMs < 0 then
  elapsedMs = 0
end

local refill = (elapsedMs / 1000.0) * refillPerSec
tokens = math.min(capacity, tokens + refill)

local allowed = 0
local remaining = tokens

if tokens >= cost then
  allowed = 1
  tokens = tokens - cost
  remaining = tokens
end

-- Save state
redis.call('HSET', key, 'tokens', tokens, 'last_refill_ms', now)

-- Compute retryAfterMs if not allowed
local retryAfterMs = 0
if allowed == 0 then
  local deficit = cost - tokens
  retryAfterMs = math.ceil((deficit / refillPerSec) * 1000.0)
end

-- Return: allowed, retryAfterMs, remaining (rounded down for API)
return { allowed, retryAfterMs, math.floor(remaining) }
