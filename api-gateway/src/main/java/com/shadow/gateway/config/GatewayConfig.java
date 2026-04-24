package com.shadow.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class GatewayConfig {

    @Value("${duplicator.url:http://localhost:8081}")
    private String duplicatorUrl;

    /**
     * All traffic is forwarded to the Traffic Duplicator, which fans out
     * to v1 (canonical) and v2 (shadow).  The gateway's job:
     *   1. Rate-limit by IP
     *   2. Add correlation ID header
     *   3. Strip sensitive headers before forwarding
     */
    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder,
                               RedisRateLimiter rateLimiter,
                               KeyResolver ipKeyResolver) {
        return builder.routes()

            // ── Orders API ──────────────────────────────────────────────────
            .route("orders", r -> r
                .path("/api/v1/orders/**")
                .filters(f -> f
                    .requestRateLimiter(c -> c
                        .setRateLimiter(rateLimiter)
                        .setKeyResolver(ipKeyResolver))
                    .addRequestHeader("X-Shadow-Source", "gateway")
                    .addRequestHeader("X-Request-Id",
                        java.util.UUID.randomUUID().toString())
                    .removeRequestHeader("Cookie"))
                .uri(duplicatorUrl))

            // ── Health / Actuator passthrough ────────────────────────────────
            .route("health", r -> r
                .path("/actuator/**")
                .uri(duplicatorUrl))

            .build();
    }

    /** 100 req/s per IP, bursting to 200 */
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(100, 200, 1);
    }

    /** Key = caller's IP address */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> Mono.just(
            exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "unknown");
    }
}
