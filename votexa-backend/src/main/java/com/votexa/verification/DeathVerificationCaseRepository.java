package com.votexa.verification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeathVerificationCaseRepository extends JpaRepository<DeathVerificationCase, Long> {
    List<DeathVerificationCase> findAllByOrderByIdDesc();
    boolean existsByVoterIdAndStatus(Long voterId, String status);
}
