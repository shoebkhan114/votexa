package com.votexa.complaint;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findAllByOrderByIdDesc();
    List<Complaint> findBySubmittedByOrderByIdDesc(Long userId);
}
