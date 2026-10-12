package com.votexa.voter;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity @Table(name = "status_changes")
@Getter @Setter @NoArgsConstructor
public class StatusChange {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "voter_id", nullable = false)
    private Long voterId;
    @Column(name = "old_status", nullable = false)
    private String oldStatus;
    @Column(name = "new_status", nullable = false)
    private String newStatus;
    @Column(nullable = false)
    private String reason;
    @Column(name = "changed_by")
    private Long changedBy;
    @Column(name = "source_ref")
    private String sourceRef;
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}
