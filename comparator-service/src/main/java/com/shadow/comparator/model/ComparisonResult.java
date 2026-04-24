package com.shadow.comparator.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "comparison_results", indexes = {
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_match", columnList = "match"),
    @Index(name = "idx_path", columnList = "path")
})
public class ComparisonResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String requestId;
    private String method;
    private String path;

    private int v1StatusCode;
    private int v2StatusCode;
    private boolean statusMatch;

    private long v1LatencyMs;
    private long v2LatencyMs;
    private long latencyDeltaMs;       // v2 - v1 (positive = v2 slower)

    private boolean bodyMatch;
    private boolean match;             // true = everything matches

    @Column(columnDefinition = "TEXT")
    private String diffSummary;        // JSON array of differences

    @Column(columnDefinition = "TEXT")
    private String v1Body;

    @Column(columnDefinition = "TEXT")
    private String v2Body;

    private boolean v1Success;
    private boolean v2Success;

    private String v1Error;
    private String v2Error;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    void prePersist() { createdAt = Instant.now(); }
}
