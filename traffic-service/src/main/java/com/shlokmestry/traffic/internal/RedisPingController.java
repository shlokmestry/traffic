package com.shlokmestry.traffic.internal;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedisPingController {

    private final StringRedisTemplate redis;

    public RedisPingController(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @GetMapping("/internal/redis/ping")
    public String ping() {
        return redis.getConnectionFactory().getConnection().ping();
    }
}
