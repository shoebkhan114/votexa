package com.votexa.election;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PollingStationRepository extends JpaRepository<PollingStation, Long> {
    Optional<PollingStation> findByCode(String code);
}
