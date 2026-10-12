package com.votexa.election;

import com.votexa.audit.AuditService;
import com.votexa.auth.AuthUser;
import com.votexa.common.ApiException;
import com.votexa.common.Hashing;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ElectionService {
    public record CreateElectionRequest(@NotBlank String code, @NotBlank String name,
                                        @NotBlank @Pattern(regexp = "LOK_SABHA|VIDHAN_SABHA|LOCAL_BODY|MOCK") String type,
                                        Instant pollingStart, Instant pollingEnd) {}
    public record ElectionDto(Long id, String code, String name, String type, String status, int rollVersion,
                              Instant pollingStart, Instant pollingEnd, String snapshotHash) {}

    private final ElectionRepository elections;
    private final JdbcTemplate jdbc;
    private final AuditService audit;

    public ElectionService(ElectionRepository elections, JdbcTemplate jdbc, AuditService audit) {
        this.elections = elections; this.jdbc = jdbc; this.audit = audit;
    }

    public static ElectionDto dto(Election e) {
        return new ElectionDto(e.getId(), e.getCode(), e.getName(), e.getType(), e.getStatus(), e.getRollVersion(),
                e.getPollingStart(), e.getPollingEnd(), e.getSnapshotHash());
    }

    public Election get(Long id) {
        return elections.findById(id).orElseThrow(() -> ApiException.notFound("Election not found"));
    }

    @Transactional(readOnly = true)
    public List<ElectionDto> list() { return elections.findAllByOrderByIdDesc().stream().map(ElectionService::dto).toList(); }

    @Transactional
    public ElectionDto create(CreateElectionRequest r, AuthUser actor) {
        if (elections.findByCode(r.code().trim()).isPresent()) throw ApiException.conflict("Election code already exists");
        if (r.pollingStart() != null && r.pollingEnd() != null && !r.pollingEnd().isAfter(r.pollingStart()))
            throw ApiException.badRequest("Polling end must be after polling start");
        Election e = new Election();
        e.setCode(r.code().trim());
        e.setName(r.name().trim());
        e.setType(r.type());
        e.setPollingStart(r.pollingStart());
        e.setPollingEnd(r.pollingEnd());
        e.setCreatedBy(actor.id());
        elections.save(e);
        audit.record("AUDIT_EVENT", "ELECTION", e.getCode(), actor, "ELECTION_CREATED", null, "DRAFT", r.name(), "SUCCESS");
        return dto(e);
    }

    /** Roll freeze: snapshot ACTIVE voters into assignments, sessions and NOT_VOTED authorization rows. */
    @Transactional
    public ElectionDto freeze(Long id, AuthUser actor) {
        Election e = get(id);
        if (!"DRAFT".equals(e.getStatus())) throw ApiException.conflict("Only a DRAFT election can be frozen (current: " + e.getStatus() + ")");
        jdbc.update("INSERT INTO voter_assignments (voter_id, election_id, polling_station_id, roll_version) " +
                "SELECT id, ?, polling_station_id, ? FROM voters WHERE status = 'ACTIVE' AND polling_station_id IS NOT NULL",
                e.getId(), e.getRollVersion());
        jdbc.update("INSERT INTO polling_sessions (election_id, polling_station_id, status, eligible_count) " +
                "SELECT election_id, polling_station_id, 'NOT_STARTED', count(*) FROM voter_assignments " +
                "WHERE election_id = ? GROUP BY election_id, polling_station_id", e.getId());
        jdbc.update("INSERT INTO voting_authorizations (election_id, polling_session_id, voter_id) " +
                "SELECT va.election_id, s.id, va.voter_id FROM voter_assignments va " +
                "JOIN polling_sessions s ON s.election_id = va.election_id AND s.polling_station_id = va.polling_station_id " +
                "WHERE va.election_id = ?", e.getId());
        List<String> rows = jdbc.queryForList("SELECT v.voter_code || '|' || p.code FROM voter_assignments va " +
                "JOIN voters v ON v.id = va.voter_id JOIN polling_stations p ON p.id = va.polling_station_id " +
                "WHERE va.election_id = ? ORDER BY 1", String.class, e.getId());
        if (rows.isEmpty()) throw ApiException.badRequest("There are no ACTIVE voters with a polling station to freeze");
        e.setSnapshotHash(Hashing.sha256Hex(String.join("\n", rows)));
        e.setStatus("ROLL_FROZEN");
        e.setFrozenAt(Instant.now());
        elections.save(e);
        audit.record("AUDIT_EVENT", "ELECTION", e.getCode(), actor, "ROLL_FROZEN", "DRAFT", "ROLL_FROZEN",
                "Roll v" + e.getRollVersion() + " frozen with " + rows.size() + " voters; snapshot " + e.getSnapshotHash().substring(0, 16), "SUCCESS");
        return dto(e);
    }

    @Transactional
    public ElectionDto openPolling(Long id, AuthUser actor) {
        Election e = get(id);
        if (!"ROLL_FROZEN".equals(e.getStatus())) throw ApiException.conflict("Roll must be frozen before polling can open");
        Instant now = Instant.now();
        if (e.getPollingStart() == null) e.setPollingStart(now);
        if (e.getPollingEnd() == null || !e.getPollingEnd().isAfter(now)) e.setPollingEnd(now.plus(10, ChronoUnit.HOURS));
        e.setStatus("POLLING_OPEN");
        elections.save(e);
        audit.record("POLLING_EVENT", "ELECTION", e.getCode(), actor, "POLLING_OPENED", "ROLL_FROZEN", "POLLING_OPEN", null, "SUCCESS");
        return dto(e);
    }

    @Transactional
    public ElectionDto closePolling(Long id, AuthUser actor) {
        Election e = get(id);
        if (!"POLLING_OPEN".equals(e.getStatus())) throw ApiException.conflict("Polling is not open");
        e.setStatus("POLLING_CLOSED");
        elections.save(e);
        audit.record("POLLING_EVENT", "ELECTION", e.getCode(), actor, "POLLING_CLOSED", "POLLING_OPEN", "POLLING_CLOSED", null, "SUCCESS");
        return dto(e);
    }

    /** Demo helper: re-centre the polling window around "now". */
    @Transactional
    public ElectionDto resetWindow(Long id, int hours, AuthUser actor) {
        Election e = get(id);
        if (!"POLLING_OPEN".equals(e.getStatus())) throw ApiException.conflict("Polling is not open");
        Instant now = Instant.now();
        e.setPollingStart(now.minus(5, ChronoUnit.MINUTES));
        e.setPollingEnd(now.plus(Math.min(Math.max(hours, 1), 72), ChronoUnit.HOURS));
        elections.save(e);
        audit.record("POLLING_EVENT", "ELECTION", e.getCode(), actor, "POLLING_WINDOW_RESET", null, hours + "h", "Window re-centred", "SUCCESS");
        return dto(e);
    }
}
