package com.votexa.polling;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface VotingAuthorizationRepository extends JpaRepository<VotingAuthorization, Long> {
    Optional<VotingAuthorization> findByElectionIdAndVoterId(Long electionId, Long voterId);
    long countByPollingSessionId(Long sessionId);
    long countByPollingSessionIdAndVotingStatus(Long sessionId, String status);
    long countByPollingSessionIdAndAuthenticatedAtIsNotNull(Long sessionId);

    /** Atomic one-time transition NOT_VOTED -> VOTED. Returns 0 if someone else already did it. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update VotingAuthorization v set v.votingStatus = 'VOTED', v.authorizedAt = :t, v.authorizedBy = :u, " +
           "v.deviceId = :d, v.authenticatedAt = coalesce(v.authenticatedAt, :t) " +
           "where v.id = :id and v.votingStatus = 'NOT_VOTED'")
    int authorize(@Param("id") Long id, @Param("t") Instant t, @Param("u") Long userId, @Param("d") String deviceId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update VotingAuthorization v set v.authenticatedAt = :t where v.id = :id and v.authenticatedAt is null")
    int markAuthenticated(@Param("id") Long id, @Param("t") Instant t);
}
