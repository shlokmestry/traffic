package com.shlokmestry.traffic.api;

import com.shlokmestry.traffic.ratelimit.TokenBucketRateLimiter;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
public class RateLimitController {

    private final TokenBucketRateLimiter limiter;

    public RateLimitController(TokenBucketRateLimiter limiter) {
        this.limiter = limiter;
    }

    @PostMapping("/check")
    public CheckRateLimitResponse check(@Valid @RequestBody CheckRateLimitRequest req) {
        // Hardcoded rule for now
        int capacity = 10;
        double refillPerSecond = 5.0;

        TokenBucketRateLimiter.Result r =
                limiter.checkAndConsume(
                        req.key(),
                        req.ruleId(),
                        capacity,
                        refillPerSecond,
                        req.cost().intValue()
                );

        return new CheckRateLimitResponse(r.allowed(), r.retryAfterMs(), r.remaining());
    }
}
