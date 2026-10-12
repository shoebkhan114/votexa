package com.votexa.election;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "polling_stations")
@Getter @Setter @NoArgsConstructor
public class PollingStation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String code;
    private String name;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "constituency_id")
    private Constituency constituency;
    private String area;
    @Column(name = "address_text")
    private String addressText;
    private Integer capacity;
    private boolean active = true;
}
