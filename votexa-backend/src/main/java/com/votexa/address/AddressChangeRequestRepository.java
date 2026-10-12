package com.votexa.address;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AddressChangeRequestRepository extends JpaRepository<AddressChangeRequest, Long> {
    List<AddressChangeRequest> findAllByOrderByIdDesc();
    List<AddressChangeRequest> findByStatusOrderByIdDesc(String status);
    List<AddressChangeRequest> findByVoterIdOrderByIdDesc(Long voterId);
    boolean existsByVoterIdAndStatusIn(Long voterId, Collection<String> statuses);
}
