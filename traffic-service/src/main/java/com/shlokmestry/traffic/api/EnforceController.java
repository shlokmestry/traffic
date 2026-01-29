package com.shlokmestry.traffic.api;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.shlokmestry.traffic.ratelimit.TokenBucketRateLimiter;
import com.shlokmestry.traffic.rules.RateLimitRule;
import com.shlokmestry.traffic.rules.RuleStore;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1")
public class EnforceController {

    private static final long RETRY_AFTER_SECONDS_IF_NO_REFILL = 3600; // 1 hour
    private static final long RETRY_AFTER_SECONDS_MAX = 86400;         // 24 hours

    private final TokenBucketRateLimiter limiter;
    private final RuleStore rules;

    public EnforceController(TokenBucketRateLimiter limiter, RuleStore rules) {
        this.limiter = limiter;
        this.rules = rules;
    }

    @PostMapping("/enforce")
    public ResponseEntity<?> enforce(@Valid @RequestBody CheckRateLimitRequest req) {
        RateLimitRule rule = rules.get(req.ruleId())
                .orElseThrow(() -> new RuleNotFound(req.ruleId())); // fail-closed

        int cost = req.cost().intValue();
        if (cost > rule.maxCost()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorBody("cost_too_high", "cost exceeds maxCost"));
        }

        TokenBucketRateLimiter.Result r = limiter.checkAndConsume(
                req.key(),
                rule.ruleId(),
                rule.capacity(),
                rule.refillTokensPerSecond(),
                cost,
                rule.ttlMs()
        );

        if (r.allowed()) {
            return ResponseEntity.noContent().build();
        }

        // If refill rate is 0, the wait time is effectively infinite; return a sane Retry-After.
        if (rule.refillTokensPerSecond() <= 0.0) {
            return ResponseEntity.status(429)
                    .header(HttpHeaders.RETRY_AFTER, String.valueOf(RETRY_AFTER_SECONDS_IF_NO_REFILL))
                    .body(new ErrorBody("rate_limited", "Too many requests"));
        }

        long retryAfterMs = r.retryAfterMs();

        long retryAfterSeconds = Math.max(1, (long) Math.ceil(retryAfterMs / 1000.0));
        retryAfterSeconds = Math.min(retryAfterSeconds, RETRY_AFTER_SECONDS_MAX);

        return ResponseEntity.status(429)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds))
                .body(new ErrorBody("rate_limited", "Too many requests"));
    }

    private record ErrorBody(String code, String message) {}

    @ResponseStatus(HttpStatus.NOT_FOUND)
    private static class RuleNotFound extends RuntimeException {
        RuleNotFound(String ruleId) { super("Rule not found: " + ruleId); }
    }
}
