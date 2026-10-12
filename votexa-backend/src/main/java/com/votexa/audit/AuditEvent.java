package com.votexa.audit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

/** Append-only (DB trigger + Hibernate @Immutable). WHO / WHAT / WHEN / WHY / RESULT. */
@Entity @Table(name = "audit_events") @Immutable
@Getter @Setter @NoArgsConstructor
public class AuditEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "event_uuid", nullable = false)
    private UUID eventUuid;
    @Column(name = "event_type", nullable = false)
    private String eventType;
    @Column(name = "entity_type", nullable = false)
    private String entityType;
    @Column(name = "entity_id", nullable = false)
    private String entityId;
    @Column(name = "actor_user_id")
    private Long actorUserId;
    @Column(name = "actor_label", nullable = false)
    private String actorLabel;
    @Column(nullable = false)
    private String action;
    @Column(name = "old_value")
    private String oldValue;
    @Column(name = "new_value")
    private String newValue;
    private String reason;
    @Column(nullable = false)
    private String result;
    @Column(name = "record_hash", nullable = false)
    private String recordHash;
    @Column(name = "previous_hash", nullable = false)
    private String previousHash;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
