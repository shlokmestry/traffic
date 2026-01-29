package com.shlokmestry.traffic.rules;

import java.util.Optional;

public interface RuleStore {
    void upsert(RateLimitRule rule);
    Optional<RateLimitRule> get(String ruleId);
}
