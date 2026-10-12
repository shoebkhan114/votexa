package com.votexa.polling;

import com.votexa.anomaly.AnomalyAlertRepository;
import com.votexa.anomaly.AnomalyService;
import com.votexa.audit.AuditService;
import com.votexa.auth.AuthUser;
import com.votexa.common.ApiException;
import com.votexa.common.Hashing;
import com.votexa.election.Election;
import com.votexa.election.ElectionRepository;
import com.votexa.election.PollingStation;
import com.votexa.election.PollingStationRepository;
import com.votexa.polling.PollingDtos.*;
import com.votexa.voter.Voter;
import com.votexa.voter.VoterRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Voter eligibility validation + one-time voting AUTHORIZATION (never the vote itself) + reconciliation.
 * Eligibility checks: exists, ACTIVE, election valid, constituency, polling station, identity, not yet authorized, polling open.
 */
@Service
public class PollingService {
    private final PollingStationRepository stations;
    private final PollingSessionRepository sessions;
    private final ElectionRepository elections;
    private final VoterRepository voters;
    private final VotingAuthorizationRepository vauths;
    private final PollingAttemptRepository attempts;
    private final AnomalyService anomalies;
    private final AnomalyAlertRepository alerts;
    private final AuditService audit;
    private final boolean enforceWindow;

    public PollingService(PollingStationRepository stations, PollingSessionRepository sessions, ElectionRepository elections,
                          VoterRepository voters, VotingAuthorizationRepository vauths, PollingAttemptRepository attempts,
                          AnomalyService anomalies, AnomalyAlertRepository alerts, AuditService audit,
                          @Value("${votexa.polling.enforce-window}") boolean enforceWindow) {
        this.stations = stations; this.sessions = sessions; this.elections = elections; this.voters = voters;
        this.vauths = vauths; this.attempts = attempts; this.anomalies = anomalies; this.alerts = alerts;
        this.audit = audit; this.enforceWindow = enforceWindow;
    }

    private record Eval(String result, String message, List<Check> checks, Voter voter, boolean showName,
                        VotingAuthorization va, PollingSession session, Election election, PollingStation station) {}

    private boolean withinWindow(Election e) {
        if (!enforceWindow) return true;
        Instant now = Instant.now();
        return (e.getPollingStart() == null || !now.isBefore(e.getPollingStart()))
                && (e.getPollingEnd() == null || !now.isAfter(e.getPollingEnd()));
    }

    private Eval evaluate(String stationCode, String voterCode, boolean identityRequired, boolean identityConfirmed) {
        PollingStation station = stations.findByCode(stationCode.trim())
                .orElseThrow(() -> ApiException.notFound("Polling station not found: " + stationCode));
        PollingSession session = sessions.findFirstByPollingStationIdOrderByIdDesc(station.getId())
                .orElseThrow(() -> ApiException.notFound("No polling session exists for station " + station.getCode()));
        Election election = elections.findById(session.getElectionId()).orElseThrow();
        Voter voter = voters.findByVoterCode(voterCode.trim()).orElse(null);
        VotingAuthorization va = voter == null ? null : vauths.findByElectionIdAndVoterId(election.getId(), voter.getId()).orElse(null);

        List<Check> checks = new ArrayList<>();
        String result = null, message = null;

        boolean exists = voter != null;
        checks.add(new Check("Voter record exists", exists));
        if (!exists) { result = "VOTER_NOT_FOUND"; message = "Voter record not found"; }

        boolean active = exists && "ACTIVE".equals(voter.getStatus());
        checks.add(new Check("Voter status is ACTIVE", active));
        if (result == null && !active) { result = "RECORD_INACTIVE"; message = "Record is " + voter.getStatus() + ": voting not permitted"; }

        boolean electionOpen = "POLLING_OPEN".equals(election.getStatus());
        checks.add(new Check("Election is valid and open", electionOpen));
        if (result == null && !electionOpen) { result = "POLLING_CLOSED"; message = "Election is not open for polling"; }

        boolean consOk = exists && voter.getConstituency().getId().equals(station.getConstituency().getId());
        checks.add(new Check("Correct constituency", consOk));
        if (result == null && !consOk) { result = "WRONG_POLLING_STATION"; message = "Voter belongs to a different constituency"; }

        boolean stationOk = va != null && va.getPollingSessionId().equals(session.getId());
        checks.add(new Check("Correct polling station", stationOk));
        if (result == null && !stationOk) {
            if (va == null) { result = "NOT_ELIGIBLE"; message = "Voter is not on the frozen roll for this election"; }
            else { result = "WRONG_POLLING_STATION"; message = "Voter is assigned to a different polling station"; }
        }

        if (identityRequired) {
            checks.add(new Check("Identity authenticated by polling officer", identityConfirmed));
            if (result == null && !identityConfirmed) { result = "NOT_ELIGIBLE"; message = "Voter identity has not been confirmed"; }
        }

        boolean notVoted = va != null && "NOT_VOTED".equals(va.getVotingStatus());
        checks.add(new Check("Voting not already authorized", notVoted));
        if (result == null && !notVoted) { result = "ALREADY_VOTED"; message = "Voting authorization was already completed for this voter"; }

        boolean windowOk = "OPEN".equals(session.getStatus()) && withinWindow(election);
        checks.add(new Check("Polling period is active", windowOk));
        if (result == null && !windowOk) {
            result = "POLLING_CLOSED";
            message = "OPEN".equals(session.getStatus()) ? "Outside the polling time window" : "Polling session is " + session.getStatus();
        }
        return new Eval(result, message, checks, voter, consOk, va, session, election, station);
    }

    private void logAttempt(Eval ev, String voterCode, String result, String detail, AuthUser actor) {
        PollingAttempt a = new PollingAttempt();
        a.setPollingSessionId(ev.session().getId());
        a.setVoterId(ev.voter() == null ? null : ev.voter().getId());
        a.setVoterCodeEntered(voterCode.length() > 20 ? voterCode.substring(0, 20) : voterCode);
        a.setResult(result);
        a.setDetail(detail);
        a.setPerformedBy(actor == null ? null : actor.id());
        attempts.save(a);
        if ("ALREADY_VOTED".equals(result) && ev.voter() != null) {
            String ref = Hashing.sha256Hex(ev.voter().getVoterCode()).substring(0, 10);
            anomalies.raiseOnce(ev.election().getId(), ev.session().getId(), "POSSIBLE_MULTIPLE_VOTING_ATTEMPT", "REVIEW",
                    "Repeated authorization attempt for voter ref " + ref + " at " + ev.station().getCode() + ". Review required.");
        }
    }

    private PollingResult toResult(Eval ev, boolean authorized) {
        boolean eligible = ev.result() == null;
        return new PollingResult(eligible ? (authorized ? "AUTHORIZED" : "ELIGIBLE") : ev.result(), eligible, authorized,
                eligible ? (authorized ? "Voting authorized. Voter marked VOTED." : "Voter is eligible") : ev.message(),
                ev.showName() ? ev.voter().getFullName() : null, ev.checks());
    }

    @Transactional
    public PollingResult validate(PollingRequest r, AuthUser actor) {
        Eval ev = evaluate(r.stationCode(), r.voterCode(), false, false);
        if (ev.result() != null) {
            logAttempt(ev, r.voterCode().trim(), ev.result(), ev.message(), actor);
        } else {
            vauths.markAuthenticated(ev.va().getId(), Instant.now());
        }
        return toResult(ev, false);
    }

    @Transactional
    public PollingResult authorize(PollingRequest r, AuthUser actor) {
        boolean confirmed = Boolean.TRUE.equals(r.identityConfirmed());
        Eval ev = evaluate(r.stationCode(), r.voterCode(), true, confirmed);
        String code = r.voterCode().trim();
        if (ev.result() != null) {
            logAttempt(ev, code, ev.result(), ev.message(), actor);
            return toResult(ev, false);
        }
        int updated = vauths.authorize(ev.va().getId(), Instant.now(), actor.id(), "ONLINE-" + ev.station().getCode());
        if (updated == 0) {   // lost a race with another authorization
            Eval again = new Eval("ALREADY_VOTED", "Voting authorization was already completed for this voter", ev.checks(),
                    ev.voter(), ev.showName(), ev.va(), ev.session(), ev.election(), ev.station());
            logAttempt(again, code, "ALREADY_VOTED", again.message(), actor);
            return toResult(again, false);
        }
        logAttempt(ev, code, "AUTHORIZED", "Voting authorized", actor);
        String ref = Hashing.sha256Hex(code).substring(0, 12);
        audit.record("POLLING_EVENT", "POLLING_SESSION", String.valueOf(ev.session().getId()), actor, "VOTING_AUTHORIZED",
                "NOT_VOTED", "VOTED", "Voter ref " + ref + " at " + ev.station().getCode(), "SUCCESS");
        return toResult(ev, true);
    }

    // ---------------- sessions, dashboard ----------------

    private SessionStats stats(PollingSession s) {
        PollingStation st = stations.findById(s.getPollingStationId()).orElseThrow();
        long eligible = vauths.countByPollingSessionId(s.getId());
        long authorized = vauths.countByPollingSessionIdAndVotingStatus(s.getId(), "VOTED");
        return new SessionStats(s.getId(), st.getCode(), st.getName(), s.getStatus(), eligible,
                vauths.countByPollingSessionIdAndAuthenticatedAtIsNotNull(s.getId()), authorized, eligible - authorized,
                alerts.countByPollingSessionIdAndStatus(s.getId(), "OPEN"), s.getRecordedCount());
    }

    private PollingSession session(Long id) {
        return sessions.findById(id).orElseThrow(() -> ApiException.notFound("Polling session not found"));
    }

    @Transactional(readOnly = true)
    public List<SessionStats> listSessions(Long electionId) {
        Long eid = electionId != null ? electionId
                : elections.findAllByOrderByIdDesc().stream().findFirst().map(Election::getId).orElse(null);
        if (eid == null) return List.of();
        return sessions.findByElectionIdOrderByPollingStationId(eid).stream().map(this::stats).toList();
    }

    @Transactional(readOnly = true)
    public SessionStats dashboard(Long sessionId) { return stats(session(sessionId)); }

    @Transactional(readOnly = true)
    public List<AttemptView> attempts(Long sessionId) {
        return attempts.findTop30ByPollingSessionIdOrderByIdDesc(sessionId).stream()
                .map(a -> new AttemptView(a.getId(), a.getVoterCodeEntered(), a.getResult(), a.getDetail(), a.getCreatedAt())).toList();
    }

    @Transactional
    public SessionStats open(Long sessionId, AuthUser actor) {
        PollingSession s = session(sessionId);
        Election e = elections.findById(s.getElectionId()).orElseThrow();
        if (!"POLLING_OPEN".equals(e.getStatus())) throw ApiException.conflict("Election polling is not open");
        if (!"NOT_STARTED".equals(s.getStatus())) throw ApiException.conflict("Session is " + s.getStatus());
        s.setStatus("OPEN");
        s.setOpenedBy(actor.id());
        s.setOpenedAt(Instant.now());
        sessions.save(s);
        audit.record("POLLING_EVENT", "POLLING_SESSION", String.valueOf(s.getId()), actor, "SESSION_OPENED",
                "NOT_STARTED", "OPEN", null, "SUCCESS");
        return stats(s);
    }

    // ---------------- reconciliation ----------------

    private Reconciliation compute(PollingSession s) {
        PollingStation st = stations.findById(s.getPollingStationId()).orElseThrow();
        long eligible = vauths.countByPollingSessionId(s.getId());
        long authorized = vauths.countByPollingSessionIdAndVotingStatus(s.getId(), "VOTED");
        Integer rec = s.getRecordedCount();
        String status, note;
        if (rec == null) { status = "PENDING"; note = "Recorded count not yet reported (session not closed)"; }
        else if (rec > authorized || authorized > eligible) {
            status = "RECONCILIATION_REQUIRED";
            note = "Expected recorded <= authorized <= eligible. Manual review required; this does not invalidate the election.";
        } else if (rec < authorized) {
            status = "RECONCILIATION_REQUIRED";
            note = "Fewer recorded than authorized. Review required.";
        } else { status = "MATCH"; note = "Recorded = authorized <= eligible"; }
        return new Reconciliation(s.getId(), st.getCode(), eligible, authorized, rec, status, note);
    }

    @Transactional(readOnly = true)
    public Reconciliation reconciliation(Long sessionId) { return compute(session(sessionId)); }

    @Transactional(readOnly = true)
    public List<Reconciliation> reconciliationAll(Long electionId) {
        Long eid = electionId != null ? electionId
                : elections.findAllByOrderByIdDesc().stream().findFirst().map(Election::getId).orElse(null);
        if (eid == null) return List.of();
        return sessions.findByElectionIdOrderByPollingStationId(eid).stream().map(this::compute).toList();
    }

    @Transactional
    public Reconciliation close(Long sessionId, int recordedCount, AuthUser actor) {
        PollingSession s = session(sessionId);
        if (!"OPEN".equals(s.getStatus())) throw ApiException.conflict("Only an OPEN session can be closed (current: " + s.getStatus() + ")");
        s.setStatus("CLOSED");
        s.setClosedBy(actor.id());
        s.setClosedAt(Instant.now());
        s.setRecordedCount(recordedCount);
        sessions.save(s);
        Reconciliation rc = compute(s);
        if ("RECONCILIATION_REQUIRED".equals(rc.status())) {
            boolean critical = rc.recorded() > rc.authorized() || rc.authorized() > rc.eligible();
            anomalies.raiseOnce(s.getElectionId(), s.getId(), "RECONCILIATION_MISMATCH", critical ? "CRITICAL" : "REVIEW",
                    "RECONCILIATION REQUIRED at " + rc.stationCode() + ": eligible=" + rc.eligible() + ", authorized="
                            + rc.authorized() + ", recorded=" + rc.recorded() + ".");
        }
        audit.record("POLLING_EVENT", "POLLING_SESSION", String.valueOf(s.getId()), actor, "SESSION_CLOSED", "OPEN", "CLOSED",
                "eligible=" + rc.eligible() + ", authorized=" + rc.authorized() + ", recorded=" + recordedCount + " -> " + rc.status(), "SUCCESS");
        return rc;
    }

    // ---------------- offline synchronization ----------------

    @Transactional
    public SyncResult sync(SyncRequest r, AuthUser actor) {
        PollingStation station = stations.findByCode(r.stationCode().trim())
                .orElseThrow(() -> ApiException.notFound("Polling station not found: " + r.stationCode()));
        PollingSession session = sessions.findFirstByPollingStationIdOrderByIdDesc(station.getId())
                .orElseThrow(() -> ApiException.notFound("No polling session for station " + station.getCode()));
        Election election = elections.findById(session.getElectionId()).orElseThrow();
        String device = r.deviceId() == null || r.deviceId().isBlank() ? "OFFLINE-" + station.getCode() : r.deviceId();

        int applied = 0, conflicts = 0, unknown = 0, wrong = 0;
        for (SyncEvent ev : r.events()) {
            Voter v = voters.findByVoterCode(ev.voterCode().trim()).orElse(null);
            VotingAuthorization va = v == null ? null : vauths.findByElectionIdAndVoterId(election.getId(), v.getId()).orElse(null);
            if (v == null || va == null) { unknown++; continue; }
            if (!va.getPollingSessionId().equals(session.getId())) { wrong++; continue; }
            Instant at;
            try { at = Instant.parse(ev.authorizedAt()); } catch (Exception e) { at = Instant.now(); }
            if (at.isAfter(Instant.now())) at = Instant.now();
            if (vauths.authorize(va.getId(), at, actor.id(), device) == 1) {
                applied++;
            } else {
                conflicts++;
                Eval fake = new Eval("ALREADY_VOTED", "Offline authorization conflicts with an existing one", List.of(), v, false,
                        va, session, election, station);
                logAttempt(fake, v.getVoterCode(), "ALREADY_VOTED", "Conflict while syncing device " + device, actor);
            }
        }
        audit.record("POLLING_EVENT", "POLLING_SESSION", String.valueOf(session.getId()), actor, "OFFLINE_SYNC_APPLIED", null,
                "applied=" + applied + ", conflicts=" + conflicts, "Device " + device + ", package " + r.packageId(), "SUCCESS");
        return new SyncResult(applied, conflicts, unknown, wrong);
    }
}
