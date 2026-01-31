package com.shlokmestry.traffic.api;

import com.shlokmestry.traffic.ratelimit.TokenBucketRateLimiter;
import com.shlokmestry.traffic.rules.RateLimitRule;
import com.shlokmestry.traffic.rules.RuleStore;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1")
public class RateLimitController {

    private final TokenBucketRateLimiter limiter;
    private final RuleStore rules;

    public RateLimitController(TokenBucketRateLimiter limiter, RuleStore rules) {
        this.limiter = limiter;
        this.rules = rules;
    }

    @PostMapping("/check")
    public CheckRateLimitResponse check(@Valid @RequestBody CheckRateLimitRequest req) {
        RateLimitRule rule = rules.get(req.ruleId())
                .orElseThrow(() -> new RuleNotFoundException(req.ruleId())); // fail-closed

        int cost = req.cost().intValue();
        if (cost > rule.maxCost()) {
            throw new CostTooHigh("cost " + cost + " exceeds maxCost " + rule.maxCost());
        }

        TokenBucketRateLimiter.Result r =
                limiter.checkAndConsume(
                        req.key(),
                        rule.ruleId(),
                        rule.endpoint(),
                        rule.plan(),
                        rule.capacity(),
                        rule.refillTokensPerSecond(),
                        rule.burstCapacity(),
                        cost,
                        rule.ttlMs()
                );

        return new CheckRateLimitResponse(r.allowed(), r.retryAfterMs(), r.remaining());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    private static class CostTooHigh extends RuntimeException {
        CostTooHigh(String msg) { super(msg); }
    }
}
