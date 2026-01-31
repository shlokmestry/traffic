package com.shlokmestry.traffic.observability;

import org.springframework.stereotype.Component;

import com.shlokmestry.traffic.rules.RateLimitRule;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class RateLimitMetrics {

    private final MeterRegistry registry;

    public RateLimitMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void decision(RateLimitRule rule, boolean allowed) {
        Counter.builder("ratelimit.decisions.total")
                .description("Total rate-limit decisions (allowed/blocked)")
                .tag("allowed", String.valueOf(allowed))
                .tag("ruleId", rule.ruleId())
                .tag("endpoint", rule.endpoint())
                .tag("plan", rule.plan())
                .register(registry)
                .increment();
    }

    public void costTooHigh(RateLimitRule rule) {
        Counter.builder("ratelimit.rejected.total")
                .description("Total rejected requests (non-rate-limit rejections)")
                .tag("reason", "cost_too_high")
                .tag("ruleId", rule.ruleId())
                .tag("endpoint", rule.endpoint())
                .tag("plan", rule.plan())
                .register(registry)
                .increment();
    }
}
