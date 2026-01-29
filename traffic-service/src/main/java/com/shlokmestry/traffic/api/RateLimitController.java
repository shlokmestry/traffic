package com.shlokmestry.traffic.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1")
public class RateLimitController {

    @PostMapping("/check")
    public CheckRateLimitResponse check(@Valid @RequestBody CheckRateLimitRequest req) {
        // STUB: always allow for now; next branch will implement Redis Lua token bucket.
        return new CheckRateLimitResponse(true, 0, -1);
    }
}
