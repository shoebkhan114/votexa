package com.votexa.polling;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PollingAttemptRepository extends JpaRepository<PollingAttempt, Long> {
    List<PollingAttempt> findTop30ByPollingSessionIdOrderByIdDesc(Long sessionId);
}
