package com.shlokmestry.traffic.api;

public record CheckRateLimitResponse(
        boolean allowed,
        long retryAfterMs,
        long remaining
) {}
