package com.shlokmestry.traffic.rules;

public record RateLimitRule(
        String ruleId,
        String endpoint,
        String plan,
        int capacity,
        double refillTokensPerSecond,
        int burstCapacity,
        long ttlMs,
        int maxCost
) {}
