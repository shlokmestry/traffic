package com.shlokmestry.traffic.api;

public record RuleResponse(
        String ruleId,
        String endpoint,
        String plan,
        int capacity,
        double refillTokensPerSecond,
        int burstCapacity,
        long ttlMs,
        int maxCost
) {}
