package com.votexa.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    Optional<AuditEvent> findTopByOrderByIdDesc();
    List<AuditEvent> findAllByOrderByIdAsc();
    List<AuditEvent> findTop200ByOrderByIdDesc();
}
