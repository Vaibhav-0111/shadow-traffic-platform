package com.shadow.analytics.service;

import com.shadow.analytics.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final JdbcTemplate jdbc;

    // ── Summary ───────────────────────────────────────────────────────────────
    public SummaryDto getSummary() {
        Long total      = jdbc.queryForObject("SELECT COUNT(*) FROM comparison_results", Long.class);
        Long matched    = jdbc.queryForObject("SELECT COUNT(*) FROM comparison_results WHERE match = true", Long.class);
        Long mismatched = jdbc.queryForObject("SELECT COUNT(*) FROM comparison_results WHERE match = false", Long.class);
        Long v2Errors   = jdbc.queryForObject("SELECT COUNT(*) FROM comparison_results WHERE v2_success = false", Long.class);
        Double avgV1    = jdbc.queryForObject("SELECT AVG(v1_latency_ms) FROM comparison_results", Double.class);
        Double avgV2    = jdbc.queryForObject("SELECT AVG(v2_latency_ms) FROM comparison_results", Double.class);
        Double avgDelta = jdbc.queryForObject("SELECT AVG(latency_delta_ms) FROM comparison_results", Double.class);

        total      = nullSafe(total);
        matched    = nullSafe(matched);
        mismatched = nullSafe(mismatched);
        v2Errors   = nullSafe(v2Errors);

        return SummaryDto.builder()
                .totalRequests(total)
                .matchedRequests(matched)
                .mismatchedRequests(mismatched)
                .mismatchRatePct(total == 0 ? 0 : (mismatched * 100.0 / total))
                .avgV1LatencyMs(avgV1 == null ? 0 : avgV1)
                .avgV2LatencyMs(avgV2 == null ? 0 : avgV2)
                .avgLatencyDeltaMs(avgDelta == null ? 0 : avgDelta)
                .v2ErrorCount(v2Errors)
                .v2ErrorRatePct(total == 0 ? 0 : (v2Errors * 100.0 / total))
                .build();
    }

    // ── Latency timeline ──────────────────────────────────────────────────────
    public List<LatencyBucket> getLatencyTimeline(int hours) {
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
        String sql = """
            SELECT DATE_TRUNC('hour', created_at) AS hour,
                   AVG(v1_latency_ms) AS avg_v1,
                   AVG(v2_latency_ms) AS avg_v2,
                   AVG(latency_delta_ms) AS avg_delta
            FROM comparison_results
            WHERE created_at > ?
            GROUP BY hour
            ORDER BY hour
        """;
        return jdbc.query(sql, ps -> ps.setTimestamp(1, Timestamp.from(since)), (rs, i) ->
                LatencyBucket.builder()
                        .hour(rs.getTimestamp("hour").toInstant())
                        .avgV1Ms(rs.getDouble("avg_v1"))
                        .avgV2Ms(rs.getDouble("avg_v2"))
                        .deltaMs(rs.getDouble("avg_delta"))
                        .build());
    }

    // ── Recent mismatches ─────────────────────────────────────────────────────
    public List<MismatchDto> getRecentMismatches(int limit) {
        String sql = """
            SELECT request_id, method, path,
                   v1_status_code, v2_status_code,
                   v1_latency_ms, v2_latency_ms,
                   diff_summary, created_at
            FROM comparison_results
            WHERE match = false
            ORDER BY created_at DESC
            LIMIT ?
        """;
        return jdbc.query(sql, ps -> ps.setInt(1, limit), (rs, i) ->
                MismatchDto.builder()
                        .requestId(rs.getString("request_id"))
                        .method(rs.getString("method"))
                        .path(rs.getString("path"))
                        .v1Status(rs.getInt("v1_status_code"))
                        .v2Status(rs.getInt("v2_status_code"))
                        .v1LatencyMs(rs.getLong("v1_latency_ms"))
                        .v2LatencyMs(rs.getLong("v2_latency_ms"))
                        .diffSummary(rs.getString("diff_summary"))
                        .createdAt(rs.getTimestamp("created_at").toInstant())
                        .build());
    }

    // ── Endpoint stats ────────────────────────────────────────────────────────
    public List<EndpointStatsDto> getEndpointStats() {
        String sql = """
            SELECT path,
                   COUNT(*) AS total,
                   SUM(CASE WHEN match = false THEN 1 ELSE 0 END) AS mismatches
            FROM comparison_results
            GROUP BY path
            ORDER BY mismatches DESC
        """;
        return jdbc.query(sql, (rs, i) -> {
            long total      = rs.getLong("total");
            long mismatches = rs.getLong("mismatches");
            return EndpointStatsDto.builder()
                    .path(rs.getString("path"))
                    .totalRequests(total)
                    .mismatches(mismatches)
                    .mismatchRatePct(total == 0 ? 0 : (mismatches * 100.0 / total))
                    .build();
        });
    }

    // ── Timeline ──────────────────────────────────────────────────────────────
    public List<TimelineBucket> getTimeline(int hours) {
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
        String sql = """
            SELECT DATE_TRUNC('hour', created_at) AS hour,
                   SUM(CASE WHEN match = true  THEN 1 ELSE 0 END) AS matched,
                   SUM(CASE WHEN match = false THEN 1 ELSE 0 END) AS mismatched
            FROM comparison_results
            WHERE created_at > ?
            GROUP BY hour ORDER BY hour
        """;
        return jdbc.query(sql, ps -> ps.setTimestamp(1, Timestamp.from(since)), (rs, i) ->
                TimelineBucket.builder()
                        .hour(rs.getTimestamp("hour").toInstant())
                        .matched(rs.getLong("matched"))
                        .mismatched(rs.getLong("mismatched"))
                        .build());
    }

    private long nullSafe(Long v) { return v == null ? 0L : v; }
}
