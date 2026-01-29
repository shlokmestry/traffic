package com.shlokmestry.traffic.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CheckRateLimitRequest(
        @NotBlank String key,          // e.g., apiKey:user123 or ip:1.2.3.4
        @NotBlank String ruleId,        // e.g., "default"
        @NotNull @Min(1) Integer cost   // tokens to consume; start with 1
) {}
