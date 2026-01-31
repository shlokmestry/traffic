package com.shlokmestry.traffic.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpsertRuleRequest(
        @NotBlank String endpoint,
        @NotBlank String plan,
        @NotNull @Min(1) Integer capacity,
        @NotNull @DecimalMin("0.0") Double refillTokensPerSecond,
        @NotNull @Min(0) Integer burstCapacity,
        @NotNull @Min(1) Long ttlMs,
        @NotNull @Min(1) Integer maxCost
) {}
