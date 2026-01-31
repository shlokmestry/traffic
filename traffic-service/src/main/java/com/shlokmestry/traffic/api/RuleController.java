package com.shlokmestry.traffic.api;

import com.shlokmestry.traffic.rules.RateLimitRule;
import com.shlokmestry.traffic.rules.RuleStore;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
                req.endpoint(),
                req.plan(),
                req.capacity(),
                req.refillTokensPerSecond(),
                req.burstCapacity(),
                req.ttlMs(),
                req.maxCost()
        );

        store.upsert(rule);

        return new RuleResponse(
                rule.ruleId(),
                rule.endpoint(),
                rule.plan(),
                rule.capacity(),
                rule.refillTokensPerSecond(),
                rule.burstCapacity(),
                rule.ttlMs(),
                rule.maxCost()
        );
    }

    @GetMapping("/{ruleId}")
    public RuleResponse get(@PathVariable @NotBlank String ruleId) {
        RateLimitRule rule = store.get(ruleId)
                .orElseThrow(() -> new RuleNotFoundException(ruleId));

        return new RuleResponse(
                rule.ruleId(),
                rule.endpoint(),
                rule.plan(),
                rule.capacity(),
                rule.refillTokensPerSecond(),
                rule.burstCapacity(),
                rule.ttlMs(),
                rule.maxCost()
        );
    }
}
