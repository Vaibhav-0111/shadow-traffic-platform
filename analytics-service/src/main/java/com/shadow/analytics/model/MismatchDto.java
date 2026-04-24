package com.shadow.analytics.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class MismatchDto {
    private String requestId;
    private String method;
    private String path;
    private int v1Status;
    private int v2Status;
    private long v1LatencyMs;
    private long v2LatencyMs;
    private String diffSummary;
    private Instant createdAt;
}
