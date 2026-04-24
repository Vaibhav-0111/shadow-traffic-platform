package com.shadow.analytics.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("service", "analytics-service");
        payload.put("message", "Service is running. Open dashboard at http://localhost:3000");

        Map<String, String> endpoints = new LinkedHashMap<>();
        endpoints.put("summary", "/api/analytics/summary");
        endpoints.put("latency", "/api/analytics/latency");
        endpoints.put("mismatches", "/api/analytics/mismatches?limit=10");
        endpoints.put("endpoints", "/api/analytics/endpoints");
        endpoints.put("timeline", "/api/analytics/timeline");

        payload.put("api", endpoints);
        return ResponseEntity.ok(payload);
    }
}
