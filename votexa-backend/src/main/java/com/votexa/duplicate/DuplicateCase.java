package com.votexa.duplicate;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name = "duplicate_cases")
@Getter @Setter @NoArgsConstructor
public class DuplicateCase {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "case_code", nullable = false)
    private String caseCode;
    @Column(name = "voter_a_id", nullable = false)
    private Long voterAId;
    @Column(name = "voter_b_id", nullable = false)
    private Long voterBId;
    @Column(name = "similarity_score", nullable = false)
    private BigDecimal similarityScore;
    @Column(name = "matched_attributes")
    private String matchedAttributes;
    @Column(nullable = false)
    private String status = "OPEN";
    @Column(name = "detected_at", insertable = false, updatable = false)
    private Instant detectedAt;
    @Column(name = "reviewed_by")
    private Long reviewedBy;
    @Column(name = "reviewed_at")
    private Instant reviewedAt;
    private String remarks;
}
