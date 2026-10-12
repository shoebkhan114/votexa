package com.votexa.blockchain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity @Table(name = "blockchain_transactions")
@Getter @Setter @NoArgsConstructor
public class BlockchainTransaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "audit_event_id", nullable = false)
    private Long auditEventId;
    @Column(name = "fabric_tx_id")
    private String fabricTxId;
    @Column(name = "event_type", nullable = false)
    private String eventType;
    @Column(name = "record_hash", nullable = false)
    private String recordHash;
    @Column(name = "previous_hash", nullable = false)
    private String previousHash;
    @Column(name = "authority_id", nullable = false)
    private String authorityId;
    private String signature;
    @Column(nullable = false)
    private String status;
    @Column(name = "integrity_status", nullable = false)
    private String integrityStatus;
    @Column(name = "retry_count", nullable = false)
    private int retryCount;
    @Column(name = "submitted_at", insertable = false, updatable = false)
    private Instant submittedAt;
    @Column(name = "committed_at")
    private Instant committedAt;
    @Column(name = "last_verified_at")
    private Instant lastVerifiedAt;
}
