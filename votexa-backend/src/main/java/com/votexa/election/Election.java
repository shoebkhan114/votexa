package com.votexa.election;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity @Table(name = "elections")
@Getter @Setter @NoArgsConstructor
public class Election {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String code;
    @Column(nullable = false)
    private String name;
    @Column(name = "election_type", nullable = false)
    private String type;
    @Column(nullable = false)
    private String status = "DRAFT";
    @Column(name = "roll_version", nullable = false)
    private int rollVersion = 1;
    @Column(name = "polling_start")
    private Instant pollingStart;
    @Column(name = "polling_end")
    private Instant pollingEnd;
    @Column(name = "snapshot_hash")
    private String snapshotHash;
    @Column(name = "frozen_at")
    private Instant frozenAt;
    @Column(name = "created_by")
    private Long createdBy;
}
