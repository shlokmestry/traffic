package com.shlokmestry.traffic.ratelimit;

import java.time.Clock;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Service
public class TokenBucketRateLimiter {

    private static final long FAIL_CLOSED_RETRY_AFTER_MS = 1000;

    private final StringRedisTemplate redis;
    private final Clock clock;
    private final RedisScript<List> script;

    private final Counter failClosedRedisError;
    private final Counter failClosedBadResponse;
    private final Counter failClosedBadTypes;

    public TokenBucketRateLimiter(StringRedisTemplate redis, MeterRegistry registry) {
        this.redis = redis;
        this.clock = Clock.systemUTC();
        this.script = RedisScript.of(new ClassPathResource("lua/token_bucket.lua"), List.class);

        this.failClosedRedisError = Counter.builder("ratelimit.fail_closed.total")
                .tag("reason", "redis_error")
                .register(registry);

        this.failClosedBadResponse = Counter.builder("ratelimit.fail_closed.total")
                .tag("reason", "bad_response")
                .register(registry);

        this.failClosedBadTypes = Counter.builder("ratelimit.fail_closed.total")
                .tag("reason", "bad_types")
                .register(registry);
    }

    public Result checkAndConsume(
            String key,
            String ruleId,
            String endpoint,
            String plan,
            int capacity,
            double refillTokensPerSecond,
            int burstCapacity,
            int cost,
            long ttlMs
    ) {
        long nowMs = clock.millis();
        String redisKey = "tb:" + ruleId + ":" + endpoint + ":" + plan + ":" + key;

        final List<?> res;
        try {
            res = redis.execute(
                    script,
                    List.of(redisKey),
                    String.valueOf(nowMs),
                    String.valueOf(capacity),
                    String.valueOf(refillTokensPerSecond),
                    String.valueOf(burstCapacity),
                    String.valueOf(cost),
                    String.valueOf(ttlMs)
            );
        } catch (Exception e) {
            failClosedRedisError.increment();
            return new Result(false, FAIL_CLOSED_RETRY_AFTER_MS, 0);
        }

        if (res == null || res.size() < 3) {
            failClosedBadResponse.increment();
            return new Result(false, FAIL_CLOSED_RETRY_AFTER_MS, 0);
        }

        try {
            boolean allowed = ((Number) res.get(0)).intValue() == 1;
            long retryAfterMs = ((Number) res.get(1)).longValue();
            long remaining = ((Number) res.get(2)).longValue();
            return new Result(allowed, retryAfterMs, remaining);
        } catch (RuntimeException e) {
            failClosedBadTypes.increment();
            return new Result(false, FAIL_CLOSED_RETRY_AFTER_MS, 0);
        }
    }

    public record Result(boolean allowed, long retryAfterMs, long remaining) {}
}
