package com.shadow.analytics.controller;

import com.shadow.analytics.model.*;
import com.shadow.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API consumed by the React dashboard.
 *
 *  GET /api/analytics/summary        → headline numbers
 *  GET /api/analytics/latency        → latency comparison over time
 *  GET /api/analytics/mismatches     → recent mismatch records
 *  GET /api/analytics/endpoints      → per-endpoint mismatch rates
 *  GET /api/analytics/timeline       → match/mismatch counts per hour
 */
@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /** Headline KPIs shown at top of dashboard */
    @GetMapping("/summary")
    public ResponseEntity<SummaryDto> summary() {
        return ResponseEntity.ok(analyticsService.getSummary());
    }

    /** Average v1 vs v2 latency bucketed by hour (last 24 h) */
    @GetMapping("/latency")
    public ResponseEntity<List<LatencyBucket>> latency(
            @RequestParam(defaultValue = "24") int hours) {
        return ResponseEntity.ok(analyticsService.getLatencyTimeline(hours));
    }

    /** Most recent N mismatches with full diff */
    @GetMapping("/mismatches")
    public ResponseEntity<List<MismatchDto>> mismatches(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(analyticsService.getRecentMismatches(limit));
    }

    /** Per-endpoint breakdown */
    @GetMapping("/endpoints")
    public ResponseEntity<List<EndpointStatsDto>> endpoints() {
        return ResponseEntity.ok(analyticsService.getEndpointStats());
    }

    /** Match / mismatch counts per hour for a sparkline chart */
    @GetMapping("/timeline")
    public ResponseEntity<List<TimelineBucket>> timeline(
            @RequestParam(defaultValue = "24") int hours) {
        return ResponseEntity.ok(analyticsService.getTimeline(hours));
    }
}
