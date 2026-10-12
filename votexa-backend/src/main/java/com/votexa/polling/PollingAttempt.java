package com.votexa.polling;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity @Table(name = "polling_attempts")
@Getter @Setter @NoArgsConstructor
public class PollingAttempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "polling_session_id", nullable = false)
    private Long pollingSessionId;
    @Column(name = "voter_id")
    private Long voterId;
    @Column(name = "voter_code_entered", nullable = false)
    private String voterCodeEntered;
    @Column(nullable = false)
    private String result;
    private String detail;
    @Column(name = "performed_by")
    private Long performedBy;
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}
