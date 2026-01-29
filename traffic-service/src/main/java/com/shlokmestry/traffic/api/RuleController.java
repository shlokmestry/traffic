package com.shlokmestry.traffic.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.shlokmestry.traffic.rules.RateLimitRule;
import com.shlokmestry.traffic.rules.RuleStore;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@RestController
@RequestMapping("/v1/rules")
public class RuleController {

    private final RuleStore store;

    public RuleController(RuleStore store) {
        this.store = store;
    }

    @PutMapping("/{ruleId}")
    @ResponseStatus(HttpStatus.OK)
    public RuleResponse upsert(
            @PathVariable @NotBlank String ruleId,
            @Valid @RequestBody UpsertRuleRequest req
    ) {
        RateLimitRule rule = new RateLimitRule(
                ruleId,
                req.capacity(),
                req.refillTokensPerSecond(),
                req.ttlMs(),
                req.maxCost()
        );
        store.upsert(rule);

        return new RuleResponse(rule.ruleId(), rule.capacity(), rule.refillTokensPerSecond(), rule.ttlMs(), rule.maxCost());
    }

    @GetMapping("/{ruleId}")
    public RuleResponse get(@PathVariable @NotBlank String ruleId) {
        RateLimitRule rule = store.get(ruleId)
                .orElseThrow(() -> new RuleNotFoundException(ruleId));
        return new RuleResponse(rule.ruleId(), rule.capacity(), rule.refillTokensPerSecond(), rule.ttlMs(), rule.maxCost());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    private static class RuleNotFoundException extends RuntimeException {
        RuleNotFoundException(String ruleId) {
            super("Rule not found: " + ruleId);
        }
    }
}
