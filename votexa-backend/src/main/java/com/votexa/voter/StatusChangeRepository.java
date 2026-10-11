package com.votexa.voter;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StatusChangeRepository extends JpaRepository<StatusChange, Long> {
    List<StatusChange> findByVoterIdOrderByIdDesc(Long voterId);
}
