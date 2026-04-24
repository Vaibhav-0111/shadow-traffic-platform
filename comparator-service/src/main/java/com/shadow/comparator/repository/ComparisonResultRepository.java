package com.shadow.comparator.repository;

import com.shadow.comparator.model.ComparisonResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ComparisonResultRepository extends JpaRepository<ComparisonResult, Long> {

    long countByMatchFalse();
    long countByMatch(boolean match);

    @Query("SELECT AVG(r.latencyDeltaMs) FROM ComparisonResult r")
    Double avgLatencyDelta();

    @Query("SELECT AVG(r.v1LatencyMs) FROM ComparisonResult r")
    Double avgV1Latency();

    @Query("SELECT AVG(r.v2LatencyMs) FROM ComparisonResult r")
    Double avgV2Latency();

    @Query("SELECT r FROM ComparisonResult r WHERE r.match = false ORDER BY r.createdAt DESC")
    List<ComparisonResult> findRecentMismatches(org.springframework.data.domain.Pageable pageable);

    @Query("SELECT r FROM ComparisonResult r WHERE r.createdAt > :since ORDER BY r.createdAt DESC")
    List<ComparisonResult> findSince(Instant since);

    @Query("SELECT r.path, COUNT(r), SUM(CASE WHEN r.match = false THEN 1 ELSE 0 END) " +
           "FROM ComparisonResult r GROUP BY r.path ORDER BY COUNT(r) DESC")
    List<Object[]> mismatchByPath();
}
