package com.shlokmestry.traffic.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.shlokmestry.traffic.observability.RateLimitMetrics;
import com.shlokmestry.traffic.ratelimit.TokenBucketRateLimiter;
import com.shlokmestry.traffic.rules.RateLimitRule;
import com.shlokmestry.traffic.rules.RuleStore;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1")
public class EnforceController {

    private static final Logger log = LoggerFactory.getLogger(EnforceController.class);

    private static final long RETRY_AFTER_SECONDS_IF_NO_REFILL = 3600; // 1 hour
    private static final long RETRY_AFTER_SECONDS_MAX = 86400; // 24 hours
    private static final long FAIL_CLOSED_RETRY_AFTER_SECONDS = 1; // 1 second

    private final TokenBucketRateLimiter limiter;
    private final RuleStore rules;
    private final RateLimitMetrics metrics;

    public EnforceController(TokenBucketRateLimiter limiter, RuleStore rules, RateLimitMetrics metrics) {
        this.limiter = limiter;
        this.rules = rules;
        this.metrics = metrics;
    }

    @PostMapping("/enforce")
    public ResponseEntity<ErrorBody> enforce(@Valid @RequestBody CheckRateLimitRequest req) {
        // FAIL-CLOSED #1: Always have a rule (even if RedisRuleStore fails)
        RateLimitRule rule;
        try {
            rule = rules.get(req.ruleId())
                    .orElseThrow(() -> new RuleNotFound(req.ruleId()));
        } catch (Exception e) {
            // RedisRuleStore.get() failed → ultra-conservative defaults
            rule = new RateLimitRule(
                    req.ruleId(),
                    "unknown",
                    "default",
                    1,
                    0.0,
                    0,
                    60000L,
                    1
            );
        }

        int cost = req.cost().intValue();
        if (cost > rule.maxCost()) {
            metrics.costTooHigh(rule);

            log.info("ratelimit reject reason=cost_too_high ruleId={} endpoint={} plan={} cost={} maxCost={}",
                    rule.ruleId(), rule.endpoint(), rule.plan(), cost, rule.maxCost());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ErrorBody("cost_too_high", "cost exceeds maxCost"));
        }

        // FAIL-CLOSED #2: Limiter failures (your existing logic)
        final TokenBucketRateLimiter.Result r;
        try {
            r = limiter.checkAndConsume(
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
        } catch (Exception e) {
            HttpHeaders h = new HttpHeaders();
            h.set("RateLimit-Limit", String.valueOf(rule.capacity()));
            h.set("RateLimit-Remaining", "0");
            h.set(HttpHeaders.RETRY_AFTER, String.valueOf(FAIL_CLOSED_RETRY_AFTER_SECONDS));
            h.set("RateLimit-Reset", String.valueOf(FAIL_CLOSED_RETRY_AFTER_SECONDS));

            log.warn("ratelimit fail_closed reason=limiter_exception ruleId={} endpoint={} plan={} cost={}",
                    rule.ruleId(), rule.endpoint(), rule.plan(), cost, e);

            return ResponseEntity.status(429)
                    .headers(h)
                    .body(new ErrorBody("rate_limited", "Rate limiter unavailable"));
        }

        metrics.decision(rule, r.allowed());

        log.info("ratelimit decision ruleId={} endpoint={} plan={} cost={} allowed={} retryAfterMs={} remaining={}",
                rule.ruleId(), rule.endpoint(), rule.plan(), cost, r.allowed(), r.retryAfterMs(), r.remaining());

        long remaining = Math.max(0, r.remaining());

        // Common headers (sent on both 204 and 429)
        HttpHeaders h = new HttpHeaders();
        h.set("RateLimit-Limit", String.valueOf(rule.capacity()));
        h.set("RateLimit-Remaining", String.valueOf(remaining));

        if (r.allowed()) {
            h.set("RateLimit-Reset", "0");
            return ResponseEntity.noContent().headers(h).build();
        }

        long retryAfterSeconds;
        if (rule.refillTokensPerSecond() <= 0.0) {
            retryAfterSeconds = RETRY_AFTER_SECONDS_IF_NO_REFILL;
        } else {
            retryAfterSeconds = Math.max(1, (long) Math.ceil(r.retryAfterMs() / 1000.0));
            retryAfterSeconds = Math.min(retryAfterSeconds, RETRY_AFTER_SECONDS_MAX);
        }

        h.set(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        h.set("RateLimit-Reset", String.valueOf(retryAfterSeconds));

        return ResponseEntity.status(429)
                .headers(h)
                .body(new ErrorBody("rate_limited", "Too many requests"));
    }

    private record ErrorBody(String code, String message) {}

    @ResponseStatus(HttpStatus.NOT_FOUND)
    private static class RuleNotFound extends RuntimeException {
        RuleNotFound(String ruleId) {
            super("Rule not found: " + ruleId);
        }
    }
}
