package com.votexa.voter;

import com.votexa.election.Constituency;
import com.votexa.election.PollingStation;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity @Table(name = "voters")
@Getter @Setter @NoArgsConstructor
public class Voter {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "voter_code", nullable = false, unique = true)
    private String voterCode;
    @Column(name = "full_name", nullable = false)
    private String fullName;
    @Column(nullable = false)
    private LocalDate dob;
    @Column(nullable = false)
    private String gender;
    @Column(nullable = false)
    private String status;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "constituency_id")
    private Constituency constituency;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "polling_station_id")
    private PollingStation pollingStation;
    @Column(name = "citizen_user_id")
    private Long citizenUserId;
    @Column(name = "registration_date", insertable = false, updatable = false)
    private LocalDate registrationDate;
    @Column(name = "record_hash")
    private String recordHash;
    @Version
    private Long version;
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}
