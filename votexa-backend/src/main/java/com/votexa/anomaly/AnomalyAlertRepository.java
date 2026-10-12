package com.votexa.anomaly;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnomalyAlertRepository extends JpaRepository<AnomalyAlert, Long> {
    List<AnomalyAlert> findTop100ByOrderByIdDesc();
    long countByPollingSessionIdAndStatus(Long sessionId, String status);
    boolean existsByPollingSessionIdAndMessageAndStatus(Long sessionId, String message, String status);
}
