package com.shlokmestry.traffic.ratelimit;

import java.time.Clock;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Service
public class TokenBucketRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(TokenBucketRateLimiter.class);
    private static final long FAIL_CLOSED_RETRY_AFTER_MS = 1000L;

    private final StringRedisTemplate redis;
    private final Clock clock;
    private final RedisScript<List> script;

    private final Counter failClosedRedisError;
    private final Counter failClosedBadResponse;
    private final Counter failClosedBadTypes;

    public TokenBucketRateLimiter(StringRedisTemplate redis, MeterRegistry meterRegistry) {
        this.redis = redis;
        this.clock = Clock.systemUTC();
        this.script = RedisScript.of(new ClassPathResource("lua/token_bucket.lua"), List.class);

        this.failClosedRedisError = Counter.builder("ratelimit.fail_closed.total")
                .tag("reason", "redis_error")
                .register(meterRegistry);

        this.failClosedBadResponse = Counter.builder("ratelimit.fail_closed.total")
                .tag("reason", "bad_response")
                .register(meterRegistry);

        this.failClosedBadTypes = Counter.builder("ratelimit.fail_closed.total")
                .tag("reason", "bad_types")
                .register(meterRegistry);
    }

    public Result checkAndConsume(
            String key,
            String ruleId,
            int capacity,
            double refillTokensPerSecond,
            int cost,
            long ttlMs
    ) {
        long nowMs = clock.millis();
        String redisKey = "tb:" + ruleId + ":" + key;

        final List<?> res;
        try {
            res = redis.execute(
                    script,
                    List.of(redisKey),
                    String.valueOf(nowMs),
                    String.valueOf(capacity),
                    String.valueOf(refillTokensPerSecond),
                    String.valueOf(cost),
                    String.valueOf(ttlMs)
            );
        } catch (Exception e) {
            failClosedRedisError.increment();
            log.warn("Fail-closed: Redis/script error for ruleId={}, key={}", ruleId, key, e);
            return new Result(false, FAIL_CLOSED_RETRY_AFTER_MS, 0);
        }

        if (res == null || res.size() < 3 || res.get(0) == null || res.get(1) == null || res.get(2) == null) {
            failClosedBadResponse.increment();
            log.warn("Fail-closed: invalid script response for ruleId={}, key={}, res={}", ruleId, key, res);
            return new Result(false, FAIL_CLOSED_RETRY_AFTER_MS, 0);
        }

        final boolean allowed;
        final long retryAfterMs;
        final long remaining;
        try {
            allowed = ((Number) res.get(0)).intValue() == 1;
            retryAfterMs = ((Number) res.get(1)).longValue();
            remaining = ((Number) res.get(2)).longValue();
        } catch (ClassCastException e) {
            failClosedBadTypes.increment();
            log.warn("Fail-closed: unexpected script response types for ruleId={}, key={}, res={}", ruleId, key, res, e);
            return new Result(false, FAIL_CLOSED_RETRY_AFTER_MS, 0);
        }

        return new Result(allowed, retryAfterMs, remaining);
    }

    public record Result(boolean allowed, long retryAfterMs, long remaining) {}
}
