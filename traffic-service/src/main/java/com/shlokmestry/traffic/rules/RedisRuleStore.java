package com.shlokmestry.traffic.rules;

import java.util.Map;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RedisRuleStore implements RuleStore {

    private static final String PREFIX = "rule:";

    private final StringRedisTemplate redis;

    public RedisRuleStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    private static String key(String ruleId) {
        return PREFIX + ruleId;
    }

    @Override
    public void upsert(RateLimitRule rule) {
        // Use a Redis hash: one key per rule, fields per attribute. [web:922]
        redis.opsForHash().putAll(
                key(rule.ruleId()),
                Map.of(
                        "capacity", String.valueOf(rule.capacity()),
                        "refillTokensPerSecond", String.valueOf(rule.refillTokensPerSecond()),
                        "ttlMs", String.valueOf(rule.ttlMs()),
                        "maxCost", String.valueOf(rule.maxCost())
                )
        );
    }

    @Override
    public Optional<RateLimitRule> get(String ruleId) {
        Map<Object, Object> m = redis.opsForHash().entries(key(ruleId));
        if (m == null || m.isEmpty()) return Optional.empty();

        int capacity = Integer.parseInt((String) m.get("capacity"));
        double refill = Double.parseDouble((String) m.get("refillTokensPerSecond"));
        long ttlMs = Long.parseLong((String) m.get("ttlMs"));
        int maxCost = Integer.parseInt((String) m.get("maxCost"));

        return Optional.of(new RateLimitRule(ruleId, capacity, refill, ttlMs, maxCost));
    }
}
