package com.votexa.voter;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    Optional<Address> findByVoterIdAndCurrentTrue(Long voterId);
    List<Address> findByCurrentTrue();
}
