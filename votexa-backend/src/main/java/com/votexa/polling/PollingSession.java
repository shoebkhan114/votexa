package com.votexa.polling;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity @Table(name = "polling_sessions")
@Getter @Setter @NoArgsConstructor
public class PollingSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "election_id", nullable = false)
    private Long electionId;
    @Column(name = "polling_station_id", nullable = false)
    private Long pollingStationId;
    @Column(nullable = false)
    private String status = "NOT_STARTED";
    @Column(name = "eligible_count", nullable = false)
    private int eligibleCount;
    @Column(name = "recorded_count")
    private Integer recordedCount;
    @Column(name = "snapshot_hash")
    private String snapshotHash;
    @Column(name = "opened_by")
    private Long openedBy;
    @Column(name = "opened_at")
    private Instant openedAt;
    @Column(name = "closed_by")
    private Long closedBy;
    @Column(name = "closed_at")
    private Instant closedAt;
}
