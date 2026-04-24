package com.shadow.duplicator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shadow.duplicator.kafka.ShadowKafkaProducer;
import com.shadow.duplicator.model.ShadowPair;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * ┌─────────────────────────────────────────────────────┐
 * │              TRAFFIC DUPLICATOR CORE                │
 * │                                                     │
 * │  Incoming Request                                   │
 * │       │                                             │
 * │       ├──────────────────────────────────────────►  │
 * │       │   (sync)  → v1 (canonical)  ──► response   │
 * │       │                                             │
 * │       └──────────────────────────────────────────►  │
 * │           (async) → v2 (shadow)     ──► /dev/null  │
 * │                                         (captured) │
 * └─────────────────────────────────────────────────────┘
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrafficDuplicatorService {

    private final WebClient.Builder webClientBuilder;
    private final ShadowKafkaProducer kafkaProducer;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;

    @Value("${services.order-v1.url:http://localhost:8082}")
    private String orderV1Url;

    @Value("${services.order-v2.url:http://localhost:8083}")
    private String orderV2Url;

    /**
     * Main fan-out method.
     *
     * @return the canonical (v1) response — the shadow result is captured async
     */
    public ResponseEntity<String> duplicate(
            String method, String path,
            String body, Map<String, String> headers) {

        String requestId = headers.getOrDefault("X-Request-Id",
                java.util.UUID.randomUUID().toString());
        Instant capturedAt = Instant.now();

        log.info("[{}] Duplicating {} {}", requestId, method, path);

        // ── 1. Call v1 synchronously (user waits for this) ─────────────────
        long v1Start = System.currentTimeMillis();
        ResponseEntity<String> canonicalResponse;
        boolean v1Success = true;
        String v1Error = null;

        try {
            canonicalResponse = callService(orderV1Url, method, path, body, headers);
        } catch (Exception e) {
            v1Success = false;
            v1Error = e.getMessage();
            log.error("[{}] v1 call failed: {}", requestId, e.getMessage());
            canonicalResponse = ResponseEntity.status(503).body("{\"error\":\"Service unavailable\"}");
        }
        long v1Latency = System.currentTimeMillis() - v1Start;

        // Capture final for lambda
        final ResponseEntity<String> finalResponse = canonicalResponse;
        final boolean finalV1Success = v1Success;
        final String finalV1Error = v1Error;

        // ── 2. Call v2 asynchronously (shadow — user never waits) ───────────
        CompletableFuture.runAsync(() -> {
            long v2Start = System.currentTimeMillis();
            boolean v2Success = true;
            String v2Error = null;
            ResponseEntity<String> shadowResponse = null;

            try {
                shadowResponse = callService(orderV2Url, method, path, body, headers);
            } catch (Exception e) {
                v2Success = false;
                v2Error = e.getMessage();
                log.warn("[{}] v2 shadow call failed: {}", requestId, e.getMessage());
            }
            long v2Latency = System.currentTimeMillis() - v2Start;

            // Record Prometheus metrics
            meterRegistry.timer("shadow.latency", "version", "v1")
                    .record(v1Latency, TimeUnit.MILLISECONDS);
            meterRegistry.timer("shadow.latency", "version", "v2")
                    .record(v2Latency, TimeUnit.MILLISECONDS);

            // ── 3. Publish full pair to Kafka for comparison ───────────────
            ShadowPair pair = ShadowPair.builder()
                    .requestId(requestId)
                    .method(method)
                    .path(path)
                    .requestBody(body)
                    .requestHeaders(headers)
                    .capturedAt(capturedAt)
                    // v1
                    .v1StatusCode(finalResponse.getStatusCode().value())
                    .v1Body(finalResponse.getBody())
                    .v1LatencyMs(v1Latency)
                    .v1Success(finalV1Success)
                    .v1Error(finalV1Error)
                    // v2
                    .v2StatusCode(shadowResponse != null ? shadowResponse.getStatusCode().value() : 0)
                    .v2Body(shadowResponse != null ? shadowResponse.getBody() : null)
                    .v2LatencyMs(v2Latency)
                    .v2Success(v2Success)
                    .v2Error(v2Error)
                    .build();

            kafkaProducer.publishShadowPair(pair);
            log.info("[{}] Shadow pair published. v1={}ms v2={}ms", requestId, v1Latency, v2Latency);

        }); // fire-and-forget

        return canonicalResponse;
    }

    private ResponseEntity<String> callService(
            String baseUrl, String method, String path,
            String body, Map<String, String> headers) {

        WebClient client = webClientBuilder.baseUrl(baseUrl).build();

        WebClient.RequestBodySpec request = client
                .method(HttpMethod.valueOf(method))
                .uri(path);

        // Forward original headers (minus hop-by-hop)
        headers.entrySet().stream()
                .filter(e -> !e.getKey().equalsIgnoreCase("host"))
                .filter(e -> !e.getKey().equalsIgnoreCase("content-length"))
                .forEach(e -> request.header(e.getKey(), e.getValue()));

        Mono<ResponseEntity<String>> mono = (body != null && !body.isBlank())
                ? request.bodyValue(body).retrieve().toEntity(String.class)
                : request.retrieve().toEntity(String.class);

        return mono
                .timeout(Duration.ofSeconds(10))
                .onErrorResume(WebClientResponseException.class, ex ->
                        Mono.just(ResponseEntity
                                .status(ex.getStatusCode())
                                .body(ex.getResponseBodyAsString())))
                .block();
    }
}
