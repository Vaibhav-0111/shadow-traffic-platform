package com.shadow.comparator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shadow.comparator.engine.JsonDiffEngine;
import com.shadow.comparator.model.ComparisonResult;
import com.shadow.comparator.repository.ComparisonResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComparisonService {

    private final JsonDiffEngine diffEngine;
    private final ComparisonResultRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * Compares the v1 and v2 halves of a shadow pair and persists a
     * {@link ComparisonResult} that the analytics service can query.
     */
    public ComparisonResult compare(Map<String, Object> pair) {
        String requestId  = str(pair, "requestId");
        String method     = str(pair, "method");
        String path       = str(pair, "path");

        int    v1Status   = intVal(pair, "v1StatusCode");
        int    v2Status   = intVal(pair, "v2StatusCode");
        long   v1Latency  = longVal(pair, "v1LatencyMs");
        long   v2Latency  = longVal(pair, "v2LatencyMs");
        String v1Body     = str(pair, "v1Body");
        String v2Body     = str(pair, "v2Body");
        boolean v1Success = boolVal(pair, "v1Success");
        boolean v2Success = boolVal(pair, "v2Success");
        String v1Error    = str(pair, "v1Error");
        String v2Error    = str(pair, "v2Error");

        // ── Status comparison ────────────────────────────────────────────────
        boolean statusMatch = (v1Status == v2Status);

        // ── Body diff ────────────────────────────────────────────────────────
        List<JsonDiffEngine.Difference> diffs = diffEngine.diff(v1Body, v2Body);
        boolean bodyMatch = diffs.isEmpty();

        // ── Overall match ────────────────────────────────────────────────────
        boolean match = statusMatch && bodyMatch && v1Success == v2Success;

        // ── Serialise diff summary ────────────────────────────────────────────
        String diffSummary;
        try {
            diffSummary = objectMapper.writeValueAsString(diffs);
        } catch (Exception e) {
            diffSummary = "[]";
        }

        ComparisonResult result = ComparisonResult.builder()
                .requestId(requestId)
                .method(method)
                .path(path)
                .v1StatusCode(v1Status)
                .v2StatusCode(v2Status)
                .statusMatch(statusMatch)
                .v1LatencyMs(v1Latency)
                .v2LatencyMs(v2Latency)
                .latencyDeltaMs(v2Latency - v1Latency)
                .bodyMatch(bodyMatch)
                .match(match)
                .diffSummary(diffSummary)
                .v1Body(v1Body)
                .v2Body(v2Body)
                .v1Success(v1Success)
                .v2Success(v2Success)
                .v1Error(v1Error)
                .v2Error(v2Error)
                .build();

        return repository.save(result);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private String str(Map<String, Object> m, String k)     { Object v = m.get(k); return v == null ? null : v.toString(); }
    private int intVal(Map<String, Object> m, String k)      { Object v = m.get(k); return v == null ? 0 : ((Number) v).intValue(); }
    private long longVal(Map<String, Object> m, String k)    { Object v = m.get(k); return v == null ? 0L : ((Number) v).longValue(); }
    private boolean boolVal(Map<String, Object> m, String k) { Object v = m.get(k); return v != null && (Boolean) v; }
}
