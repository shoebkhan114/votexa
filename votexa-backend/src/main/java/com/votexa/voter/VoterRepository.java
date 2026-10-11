package com.votexa.voter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VoterRepository extends JpaRepository<Voter, Long> {
    Optional<Voter> findByVoterCode(String voterCode);
    Optional<Voter> findByCitizenUserId(Long userId);
    java.util.List<Voter> findByDob(java.time.LocalDate dob);
    long countByPollingStationIdAndStatus(Long stationId, String status);

    /** q and status are never null (use "" for "no filter") to avoid untyped-null parameters in PostgreSQL. */
    @Query("select v from Voter v where (:status = '' or v.status = :status) and " +
           "(:q = '' or lower(v.fullName) like lower(concat('%', :q, '%')) or lower(v.voterCode) like lower(concat('%', :q, '%')))")
    Page<Voter> search(@Param("q") String q, @Param("status") String status, Pageable pageable);
}
