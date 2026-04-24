package com.shadow.analytics.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EndpointStatsDto {
    private String path;
    private long totalRequests;
    private long mismatches;
    private double mismatchRatePct;
}
