package com.shlokmestry.traffic.api;

public record RuleResponse(
        String ruleId,
        int capacity,
        double refillTokensPerSecond,
        long ttlMs,
        int maxCost
) {}
