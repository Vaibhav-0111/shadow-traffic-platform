package com.shadow.analytics.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class LatencyBucket {
    private Instant hour;
    private double avgV1Ms;
    private double avgV2Ms;
    private double deltaMs;
}
