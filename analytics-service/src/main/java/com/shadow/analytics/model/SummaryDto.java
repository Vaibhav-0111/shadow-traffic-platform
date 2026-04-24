package com.shadow.analytics.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SummaryDto {
    private long totalRequests;
    private long matchedRequests;
    private long mismatchedRequests;
    private double mismatchRatePct;
    private double avgV1LatencyMs;
    private double avgV2LatencyMs;
    private double avgLatencyDeltaMs;
    private long v2ErrorCount;
    private double v2ErrorRatePct;
}
