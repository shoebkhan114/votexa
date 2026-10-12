package com.votexa.voter;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity @Table(name = "addresses")
@Getter @Setter @NoArgsConstructor
public class Address {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "voter_id", nullable = false)
    private Long voterId;
    @Column(nullable = false)
    private String line1;
    @Column(nullable = false)
    private String locality;
    @Column(nullable = false)
    private String district;
    @Column(nullable = false)
    private String state;
    @Column(nullable = false)
    private String pincode;
    @Column(name = "is_current", nullable = false)
    private boolean current = true;
    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom = LocalDate.now();
    @Column(name = "valid_to")
    private LocalDate validTo;
}
