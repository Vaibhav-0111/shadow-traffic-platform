package com.shadow.duplicator.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

/**
 * Represents a fully captured shadow traffic pair — the canonical (v1) response
 * and the shadow (v2) response — along with metadata for comparison.
 */
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ShadowPair {

    private String requestId;
    private String method;
    private String path;
    private String requestBody;
    private Map<String, String> requestHeaders;
    private Instant capturedAt;

    // Canonical (v1) result
    private int v1StatusCode;
    private String v1Body;
    private long v1LatencyMs;
    private boolean v1Success;
    private String v1Error;

    // Shadow (v2) result
    private int v2StatusCode;
    private String v2Body;
    private long v2LatencyMs;
    private boolean v2Success;
    private String v2Error;
}
