package com.votexa.auth;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity @Table(name = "users")
@Getter @Setter @NoArgsConstructor
public class UserAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String username;
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Column(name = "full_name", nullable = false)
    private String fullName;
    private String email;
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "role_id")
    private Role role;
    @Column(name = "constituency_id")
    private Long constituencyId;
    private boolean enabled = true;
    @Column(name = "failed_login_attempts")
    private int failedLoginAttempts;
    @Column(name = "last_login_at")
    private Instant lastLoginAt;
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;
}
