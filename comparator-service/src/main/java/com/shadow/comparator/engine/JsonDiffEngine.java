package com.shadow.comparator.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Deep JSON diff engine.
 *
 * Given two JSON strings it produces a list of {@link Difference} objects
 * describing every field that is missing, added, or has a different value.
 *
 * Works recursively on nested objects and arrays.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JsonDiffEngine {

    private final ObjectMapper objectMapper;

    public List<Difference> diff(String json1, String json2) {
        List<Difference> diffs = new ArrayList<>();

        if (json1 == null && json2 == null) return diffs;
        if (json1 == null) { diffs.add(new Difference("$", null, json2, DiffType.ADDED)); return diffs; }
        if (json2 == null) { diffs.add(new Difference("$", json1, null, DiffType.REMOVED)); return diffs; }

        try {
            JsonNode node1 = objectMapper.readTree(json1);
            JsonNode node2 = objectMapper.readTree(json2);
            diffNodes("$", node1, node2, diffs);
        } catch (Exception e) {
            // Fallback to plain string comparison for non-JSON bodies
            if (!json1.equals(json2)) {
                diffs.add(new Difference("$", json1, json2, DiffType.VALUE_CHANGED));
            }
        }

        return diffs;
    }

    private void diffNodes(String path, JsonNode n1, JsonNode n2, List<Difference> diffs) {
        if (n1.isObject() && n2.isObject()) {
            diffObjects(path, n1, n2, diffs);
        } else if (n1.isArray() && n2.isArray()) {
            diffArrays(path, n1, n2, diffs);
        } else if (!n1.equals(n2)) {
            diffs.add(new Difference(path, n1.toString(), n2.toString(), DiffType.VALUE_CHANGED));
        }
    }

    private void diffObjects(String path, JsonNode n1, JsonNode n2, List<Difference> diffs) {
        Set<String> keys1 = new HashSet<>();
        n1.fieldNames().forEachRemaining(keys1::add);

        Set<String> keys2 = new HashSet<>();
        n2.fieldNames().forEachRemaining(keys2::add);

        // Fields in v1 but not v2
        for (String key : keys1) {
            if (!keys2.contains(key)) {
                diffs.add(new Difference(path + "." + key,
                        n1.get(key).toString(), null, DiffType.REMOVED));
            } else {
                diffNodes(path + "." + key, n1.get(key), n2.get(key), diffs);
            }
        }

        // Fields in v2 but not v1
        for (String key : keys2) {
            if (!keys1.contains(key)) {
                diffs.add(new Difference(path + "." + key,
                        null, n2.get(key).toString(), DiffType.ADDED));
            }
        }
    }

    private void diffArrays(String path, JsonNode n1, JsonNode n2, List<Difference> diffs) {
        int maxLen = Math.max(n1.size(), n2.size());
        for (int i = 0; i < maxLen; i++) {
            String iPath = path + "[" + i + "]";
            if (i >= n1.size()) {
                diffs.add(new Difference(iPath, null, n2.get(i).toString(), DiffType.ADDED));
            } else if (i >= n2.size()) {
                diffs.add(new Difference(iPath, n1.get(i).toString(), null, DiffType.REMOVED));
            } else {
                diffNodes(iPath, n1.get(i), n2.get(i), diffs);
            }
        }
    }

    // ── DTOs ─────────────────────────────────────────────────────────────────

    public record Difference(String path, String v1Value, String v2Value, DiffType type) {}

    public enum DiffType { VALUE_CHANGED, ADDED, REMOVED }
}
