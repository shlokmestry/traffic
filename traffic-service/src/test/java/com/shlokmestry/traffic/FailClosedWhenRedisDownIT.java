package com.shlokmestry.traffic;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;

import com.shlokmestry.traffic.rules.RateLimitRule;
import com.shlokmestry.traffic.rules.RuleStore;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        // Force Redis to be unreachable so limiter must fail-closed
        "spring.data.redis.host=127.0.0.1",
        "spring.data.redis.port=6390"
})
class FailClosedWhenRedisDownIT {

    @TestConfiguration
    static class InMemoryRuleStoreConfig {
        @Bean
        @Primary
        RuleStore ruleStore() {
            return new RuleStore() {
                private final ConcurrentHashMap<String, RateLimitRule> rules = new ConcurrentHashMap<>();

                @Override
                public void upsert(RateLimitRule rule) {
                    rules.put(rule.ruleId(), rule);
                }

                @Override
                public Optional<RateLimitRule> get(String ruleId) {
                    return Optional.ofNullable(rules.get(ruleId));
                }
            };
        }
    }

    @LocalServerPort
    int port;

    @Autowired
    RuleStore ruleStore;

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void enforce_returns429_whenRedisUnavailable() throws Exception {
        // Rule exists => controller will reach limiter (not 404).
        // Phase 5 constructor: ruleId, endpoint, plan, capacity, refill, burst, ttlMs, maxCost
        ruleStore.upsert(new RateLimitRule("rule-1", "test-endpoint", "default", 5, 1.0, 0, 60_000L, 5));

        // Build JSON manually to avoid needing Jackson on the test classpath
        String json = """
                {"key":"user-123","ruleId":"rule-1","cost":1}
                """;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/v1/enforce"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

        assertThat(resp.statusCode()).isEqualTo(429); // fail-closed surfaces as 429
        assertThat(resp.headers().firstValue(HttpHeaders.RETRY_AFTER)).isPresent(); // controller sets it on 429
    }
}
