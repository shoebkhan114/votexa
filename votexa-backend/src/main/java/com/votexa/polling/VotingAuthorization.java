package com.votexa.polling;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** Eligibility + authorization state only. Never linked to a candidate choice. */
@Entity @Table(name = "voting_authorizations")
@Getter @Setter @NoArgsConstructor
public class VotingAuthorization {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "election_id", nullable = false)
    private Long electionId;
    @Column(name = "polling_session_id", nullable = false)
    private Long pollingSessionId;
    @Column(name = "voter_id", nullable = false)
    private Long voterId;
    @Column(name = "voting_status", nullable = false)
    private String votingStatus = "NOT_VOTED";
    @Column(name = "authenticated_at")
    private Instant authenticatedAt;
    @Column(name = "authorized_at")
    private Instant authorizedAt;
    @Column(name = "authorized_by")
    private Long authorizedBy;
    @Column(name = "device_id")
    private String deviceId;
}
