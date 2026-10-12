package com.votexa.election;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Table(name = "constituencies")
@Getter @Setter @NoArgsConstructor
public class Constituency {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String code;
    private String name;
    private String district;
    private String state;
}
