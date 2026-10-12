package com.votexa.anomaly;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity @Table(name = "anomaly_alerts")
@Getter @Setter @NoArgsConstructor
public class AnomalyAlert {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "election_id", nullable = false)
    private Long electionId;
    @Column(name = "polling_session_id")
    private Long pollingSessionId;
    @Column(name = "alert_type", nullable = false)
    private String alertType;
    @Column(nullable = false)
    private String severity;
    @Column(nullable = false)
    private String message;
    @Column(nullable = false)
    private String status = "OPEN";
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "resolved_by")
    private Long resolvedBy;
    @Column(name = "resolved_at")
    private Instant resolvedAt;
}
