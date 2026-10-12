package com.votexa.duplicate;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DuplicateCaseRepository extends JpaRepository<DuplicateCase, Long> {
    List<DuplicateCase> findAllByOrderByIdDesc();
    List<DuplicateCase> findByStatusOrderByIdDesc(String status);
    boolean existsByVoterAIdAndVoterBId(Long a, Long b);
}
