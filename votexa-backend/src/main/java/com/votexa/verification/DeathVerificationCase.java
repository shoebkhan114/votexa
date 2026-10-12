package com.votexa.verification;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity @Table(name = "death_verification_cases")
@Getter @Setter @NoArgsConstructor
public class DeathVerificationCase {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "case_code", nullable = false)
    private String caseCode;
    @Column(name = "voter_id", nullable = false)
    private Long voterId;
    @Column(nullable = false)
    private String source;
    @Column(name = "registry_ref_hash")
    private String registryRefHash;
    @Column(nullable = false)
    private String status = "OPEN";
    @Column(name = "verified_by")
    private Long verifiedBy;
    @Column(name = "reviewed_by")
    private Long reviewedBy;
    @Column(name = "reviewed_at")
    private Instant reviewedAt;
    private String remarks;
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}
