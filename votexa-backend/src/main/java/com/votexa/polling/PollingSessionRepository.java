package com.votexa.polling;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PollingSessionRepository extends JpaRepository<PollingSession, Long> {
    Optional<PollingSession> findFirstByPollingStationIdOrderByIdDesc(Long stationId);
    List<PollingSession> findByElectionIdOrderByPollingStationId(Long electionId);
}
