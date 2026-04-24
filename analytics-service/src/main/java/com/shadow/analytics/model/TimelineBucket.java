package com.shadow.analytics.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class TimelineBucket {
    private Instant hour;
    private long matched;
    private long mismatched;
}
