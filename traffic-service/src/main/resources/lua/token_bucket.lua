-- KEYS[1] = bucket key (hash): tb:ruleId:endpoint:plan:key
-- ARGV[1] = nowMs
-- ARGV[2] = capacity (integer)
-- ARGV[3] = refillTokensPerSecond (number, can be fractional)  
-- ARGV[4] = burstCapacity (integer) - PHASE 5 NEW
-- ARGV[5] = cost (integer)
-- ARGV[6] = ttlMs (integer)

local key = KEYS[1]

local now = tonumber(ARGV[1])
local capacity = tonumber(ARGV[2])
local refillPerSec = tonumber(ARGV[3])
local burst_capacity = tonumber(ARGV[4])  -- PHASE 5: Burst allowance
local cost = tonumber(ARGV[5])
local ttlMs = tonumber(ARGV[6])

local tokensStr = redis.call('HGET', key, 'tokens')
local lastRefillStr = redis.call('HGET', key, 'last_refill_ms')

-- PHASE 5: Initialize to burst_capacity instead of regular capacity
local tokens = tokensStr and tonumber(tokensStr) or burst_capacity
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

-- Ensure the bucket expires so Redis doesn't grow forever.
if ttlMs and ttlMs > 0 then
  redis.call('PEXPIRE', key, ttlMs)
end

-- Compute retryAfterMs if not allowed
local retryAfterMs = 0
if allowed == 0 then
  local deficit = cost - tokens
  retryAfterMs = math.ceil((deficit / refillPerSec) * 1000.0)
end

-- Return: allowed, retryAfterMs, remaining (rounded down for API)
return { allowed, retryAfterMs, math.floor(remaining) }
