package com.votexa.election;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ConstituencyRepository extends JpaRepository<Constituency, Long> {
    Optional<Constituency> findByCode(String code);
}
