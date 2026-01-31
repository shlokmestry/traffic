package com.shlokmestry.traffic.rules;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RedisRuleStore implements RuleStore {

    private static final String PREFIX = "rule:";
    private static final String DEFAULT_ENDPOINT = "unknown";
    private static final String DEFAULT_PLAN = "default";

    private final StringRedisTemplate redis;

    public RedisRuleStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    private static String key(String ruleId) {
        return PREFIX + ruleId;
    }

    @Override
    public void upsert(RateLimitRule rule) {
        // Map.of() rejects nulls, so build a mutable map safely.
        Map<String, String> fields = new HashMap<>();
        fields.put("endpoint", rule.endpoint() == null ? DEFAULT_ENDPOINT : rule.endpoint());
        fields.put("plan", rule.plan() == null ? DEFAULT_PLAN : rule.plan());
        fields.put("capacity", String.valueOf(rule.capacity()));
        fields.put("refillTokensPerSecond", String.valueOf(rule.refillTokensPerSecond()));
        fields.put("burstCapacity", String.valueOf(rule.burstCapacity()));
        fields.put("ttlMs", String.valueOf(rule.ttlMs()));
        fields.put("maxCost", String.valueOf(rule.maxCost()));

        redis.opsForHash().putAll(key(rule.ruleId()), fields);
    }

    @Override
    public Optional<RateLimitRule> get(String ruleId) {
        Map<Object, Object> m = redis.opsForHash().entries(key(ruleId));
        if (m == null || m.isEmpty()) return Optional.empty();

        // Backward-compatible defaults if older rules were stored without new fields
        String endpoint = (String) m.getOrDefault("endpoint", DEFAULT_ENDPOINT);
        String plan = (String) m.getOrDefault("plan", DEFAULT_PLAN);

        int capacity = Integer.parseInt((String) m.get("capacity"));
        double refill = Double.parseDouble((String) m.get("refillTokensPerSecond"));
        long ttlMs = Long.parseLong((String) m.get("ttlMs"));
        int maxCost = Integer.parseInt((String) m.get("maxCost"));

        String burstStr = (String) m.get("burstCapacity");
        int burst = (burstStr == null) ? capacity : Integer.parseInt(burstStr);

        return Optional.of(new RateLimitRule(ruleId, endpoint, plan, capacity, refill, burst, ttlMs, maxCost));
    }
}
