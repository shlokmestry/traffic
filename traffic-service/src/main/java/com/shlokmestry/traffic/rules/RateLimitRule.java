package com.shlokmestry.traffic.rules;

public record RateLimitRule(
        String ruleId,
        int capacity,
        double refillTokensPerSecond,
        long ttlMs,
        int maxCost
) {}
