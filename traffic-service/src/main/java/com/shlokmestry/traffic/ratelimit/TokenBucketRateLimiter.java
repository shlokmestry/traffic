package com.shlokmestry.traffic.ratelimit;

import java.time.Clock;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

@Service
public class TokenBucketRateLimiter {

    private final StringRedisTemplate redis;
    private final Clock clock;
    private final RedisScript<List> script;

    public TokenBucketRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
        this.clock = Clock.systemUTC();
        this.script = RedisScript.of(new ClassPathResource("lua/token_bucket.lua"), List.class);
    }

    public Result checkAndConsume(String key, String ruleId, int capacity, double refillTokensPerSecond, int cost) {
        long nowMs = clock.millis();
        String redisKey = "tb:" + ruleId + ":" + key;

        List<?> res = redis.execute(
                script,
                List.of(redisKey),
                String.valueOf(nowMs),
                String.valueOf(capacity),
                String.valueOf(refillTokensPerSecond),
                String.valueOf(cost)
        );

        if (res == null || res.size() < 3) {
            return new Result(true, 0, -1); // fail-open for now
        }

        boolean allowed = ((Number) res.get(0)).intValue() == 1;
        long retryAfterMs = ((Number) res.get(1)).longValue();
        long remaining = ((Number) res.get(2)).longValue();
        return new Result(allowed, retryAfterMs, remaining);
    }

    public record Result(boolean allowed, long retryAfterMs, long remaining) {}
}
