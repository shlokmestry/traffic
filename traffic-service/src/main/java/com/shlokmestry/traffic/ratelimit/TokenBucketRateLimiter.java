package com.shlokmestry.traffic.ratelimit;

import java.time.Clock;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

@Service
public class TokenBucketRateLimiter {

    private static final long FAIL_CLOSED_RETRY_AFTER_MS = 1000L;

    private final StringRedisTemplate redis;
    private final Clock clock;
    private final RedisScript<List> script;

    public TokenBucketRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
        this.clock = Clock.systemUTC();
        this.script = RedisScript.of(new ClassPathResource("lua/token_bucket.lua"), List.class);
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
            // Fail-closed: if Redis/script execution fails, deny the request.
            return new Result(false, FAIL_CLOSED_RETRY_AFTER_MS, 0);
        }

        // Fail-closed: if we can’t read a valid response, deny the request.
        if (res == null || res.size() < 3 || res.get(0) == null || res.get(1) == null || res.get(2) == null) {
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
            // Fail-closed: unexpected types from Lua/Redis.
            return new Result(false, FAIL_CLOSED_RETRY_AFTER_MS, 0);
        }

        return new Result(allowed, retryAfterMs, remaining);
    }

    public record Result(boolean allowed, long retryAfterMs, long remaining) {}
}
