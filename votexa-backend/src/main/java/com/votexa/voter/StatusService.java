package com.votexa.voter;

import com.votexa.audit.AuditService;
import com.votexa.auth.AuthUser;
import com.votexa.common.ApiException;
import com.votexa.common.Hashing;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/** Single place where a voter's lifecycle status changes (history + audit + fingerprint). */
@Service
public class StatusService {
    public static final Set<String> VALID = Set.of("ACTIVE", "PENDING", "UNDER_VERIFICATION", "SHIFTED",
            "DECEASED", "DUPLICATE_REVIEW", "INACTIVE", "BLOCKED");

    private final VoterRepository voters;
    private final StatusChangeRepository changes;
    private final AuditService audit;

    public StatusService(VoterRepository voters, StatusChangeRepository changes, AuditService audit) {
        this.voters = voters; this.changes = changes; this.audit = audit;
    }

    public static void rehash(Voter v) {
        v.setRecordHash(Hashing.sha256Hex(v.getVoterCode() + "|" + v.getFullName() + "|" + v.getDob() + "|" + v.getStatus()));
    }

    @Transactional
    public void change(Voter v, String newStatus, String reason, AuthUser actor, String sourceRef) {
        if (!VALID.contains(newStatus)) throw ApiException.badRequest("Invalid status: " + newStatus);
        String old = v.getStatus();
        if (old.equals(newStatus)) return;
        if (reason == null || reason.isBlank()) throw ApiException.badRequest("A reason is required for a status change");
        if ("ACTIVE".equals(newStatus) && v.getPollingStation() == null)
            throw ApiException.badRequest("An ACTIVE voter needs a polling station assigned");

        StatusChange sc = new StatusChange();
        sc.setVoterId(v.getId());
        sc.setOldStatus(old);
        sc.setNewStatus(newStatus);
        sc.setReason(reason);
        sc.setChangedBy(actor == null ? null : actor.id());
        sc.setSourceRef(sourceRef);
        changes.save(sc);

        v.setStatus(newStatus);
        rehash(v);
        voters.save(v);
        audit.record("STATUS_CHANGE", "VOTER", v.getVoterCode(), actor, "STATUS_CHANGED", old, newStatus, reason, "SUCCESS");
    }
}
