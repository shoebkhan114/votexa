package com.votexa.verification;

import com.votexa.audit.AuditService;
import com.votexa.auth.AuthUser;
import com.votexa.common.ApiException;
import com.votexa.common.Hashing;
import com.votexa.voter.StatusChange;
import com.votexa.voter.StatusChangeRepository;
import com.votexa.voter.StatusService;
import com.votexa.voter.Voter;
import com.votexa.voter.VoterRepository;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Deceased / inactive workflow: possible match -> verification -> officer review -> status change -> audit. No auto-deletion. */
@Service
public class DeathService {
    public record DeathCaseDto(Long id, String caseCode, String voterCode, String voterName, String source, String status,
                               boolean verified, String remarks, Instant createdAt) {}
    public record ReviewRequest(@NotBlank @Pattern(regexp = "CONFIRM|REJECT") String decision, String remarks) {}

    private final DeathVerificationCaseRepository cases;
    private final VoterRepository voters;
    private final StatusChangeRepository changes;
    private final StatusService statusService;
    private final AuditService audit;

    public DeathService(DeathVerificationCaseRepository cases, VoterRepository voters, StatusChangeRepository changes,
                        StatusService statusService, AuditService audit) {
        this.cases = cases; this.voters = voters; this.changes = changes; this.statusService = statusService; this.audit = audit;
    }

    private DeathCaseDto toDto(DeathVerificationCase c) {
        Voter v = voters.findById(c.getVoterId()).orElseThrow();
        return new DeathCaseDto(c.getId(), c.getCaseCode(), v.getVoterCode(), v.getFullName(), c.getSource(), c.getStatus(),
                c.getVerifiedBy() != null, c.getRemarks(), c.getCreatedAt());
    }

    @Transactional
    public DeathCaseDto open(String voterCode, String source, AuthUser actor) {
        Voter v = voters.findByVoterCode(voterCode.trim()).orElseThrow(() -> ApiException.notFound("Voter not found"));
        if ("DECEASED".equals(v.getStatus())) throw ApiException.conflict("Voter is already marked DECEASED");
        if (cases.existsByVoterIdAndStatus(v.getId(), "OPEN")) throw ApiException.conflict("An open case already exists for this voter");
        DeathVerificationCase c = new DeathVerificationCase();
        c.setCaseCode("TMP-" + UUID.randomUUID().toString().substring(0, 12));
        c.setVoterId(v.getId());
        c.setSource(source);
        c.setRegistryRefHash(Hashing.sha256Hex("mock-registry-ref|" + UUID.randomUUID()));
        cases.save(c);
        c.setCaseCode(String.format("DTH-%04d", c.getId()));
        cases.save(c);
        statusService.change(v, "UNDER_VERIFICATION", "Possible death-registry match (" + c.getCaseCode() + ")", actor, c.getCaseCode());
        return toDto(c);
    }

    @Transactional(readOnly = true)
    public List<DeathCaseDto> list() { return cases.findAllByOrderByIdDesc().stream().map(this::toDto).toList(); }

    @Transactional
    public DeathCaseDto verify(Long id, String remarks, AuthUser actor) {
        DeathVerificationCase c = cases.findById(id).orElseThrow(() -> ApiException.notFound("Case not found"));
        if (!"OPEN".equals(c.getStatus())) throw ApiException.conflict("Case already closed");
        c.setVerifiedBy(actor.id());
        if (remarks != null && !remarks.isBlank()) c.setRemarks(remarks);
        cases.save(c);
        audit.record("VOTER_VERIFICATION", "DEATH_CASE", c.getCaseCode(), actor, "DEATH_FIELD_VERIFIED", "OPEN", "VERIFIED", remarks, "SUCCESS");
        return toDto(c);
    }

    @Transactional
    public DeathCaseDto review(Long id, ReviewRequest req, AuthUser actor) {
        DeathVerificationCase c = cases.findById(id).orElseThrow(() -> ApiException.notFound("Case not found"));
        if (!"OPEN".equals(c.getStatus())) throw ApiException.conflict("Case already closed");
        if (c.getVerifiedBy() == null) throw ApiException.badRequest("Field verification is required before officer review");
        Voter v = voters.findById(c.getVoterId()).orElseThrow();
        boolean confirm = "CONFIRM".equals(req.decision());
        if (confirm) {
            c.setStatus("VERIFIED");
            statusService.change(v, "DECEASED", "Death verified (" + c.getCaseCode() + ")", actor, c.getCaseCode());
        } else {
            c.setStatus("REJECTED");
            String back = changes.findByVoterIdOrderByIdDesc(v.getId()).stream()
                    .filter(s -> "UNDER_VERIFICATION".equals(s.getNewStatus())).map(StatusChange::getOldStatus).findFirst()
                    .orElse(v.getPollingStation() != null ? "ACTIVE" : "PENDING");
            if ("ACTIVE".equals(back) && v.getPollingStation() == null) back = "PENDING";
            statusService.change(v, back, "Death match rejected (" + c.getCaseCode() + ")", actor, c.getCaseCode());
        }
        c.setReviewedBy(actor.id());
        c.setReviewedAt(Instant.now());
        c.setRemarks(req.remarks());
        cases.save(c);
        return toDto(c);
    }
}
