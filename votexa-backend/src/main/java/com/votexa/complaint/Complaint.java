package com.votexa.complaint;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity @Table(name = "complaints")
@Getter @Setter @NoArgsConstructor
public class Complaint {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "case_code", nullable = false)
    private String caseCode;
    @Column(name = "voter_id")
    private Long voterId;
    @Column(name = "submitted_by", nullable = false)
    private Long submittedBy;
    @Column(nullable = false)
    private String category;
    @Column(nullable = false, columnDefinition = "text")
    private String description;
    @Column(nullable = false)
    private String status = "OPEN";
    @Column(name = "assigned_to")
    private Long assignedTo;
    @Column(columnDefinition = "text")
    private String decision;
    @Column(name = "decided_at")
    private Instant decidedAt;
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}
