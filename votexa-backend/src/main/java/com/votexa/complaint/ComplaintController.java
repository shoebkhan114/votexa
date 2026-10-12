package com.votexa.complaint;

import com.votexa.audit.AuditService;
import com.votexa.auth.AuthUser;
import com.votexa.common.ApiException;
import com.votexa.common.Roles;
import com.votexa.voter.Voter;
import com.votexa.voter.VoterRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {
    public record CreateRequest(@NotBlank @Pattern(regexp = "WRONG_ADDRESS|DUPLICATE_RECORD|INCORRECT_INFORMATION|WRONG_POLLING_STATION|DECEASED_PERSON_LISTED|OTHER") String category,
                                @NotBlank String description) {}
    public record DecisionRequest(@NotBlank @Pattern(regexp = "UNDER_REVIEW|RESOLVED|REJECTED") String status, String decision) {}
    public record ComplaintDto(Long id, String caseCode, String voterCode, String category, String description,
                               String status, String decision, Instant createdAt) {}

    private final ComplaintRepository complaints;
    private final VoterRepository voters;
    private final AuditService audit;

    public ComplaintController(ComplaintRepository complaints, VoterRepository voters, AuditService audit) {
        this.complaints = complaints; this.voters = voters; this.audit = audit;
    }

    private ComplaintDto toDto(Complaint c) {
        String code = c.getVoterId() == null ? null : voters.findById(c.getVoterId()).map(Voter::getVoterCode).orElse(null);
        return new ComplaintDto(c.getId(), c.getCaseCode(), code, c.getCategory(), c.getDescription(), c.getStatus(), c.getDecision(), c.getCreatedAt());
    }

    @PostMapping
    @PreAuthorize(Roles.CITIZEN)
    @Transactional
    public ComplaintDto create(@Valid @RequestBody CreateRequest r, @AuthenticationPrincipal AuthUser me) {
        Complaint c = new Complaint();
        c.setCaseCode("TMP-" + UUID.randomUUID().toString().substring(0, 12));
        c.setSubmittedBy(me.id());
        voters.findByCitizenUserId(me.id()).ifPresent(v -> c.setVoterId(v.getId()));
        c.setCategory(r.category());
        c.setDescription(r.description().trim());
        complaints.save(c);
        c.setCaseCode(String.format("CMP-%04d", c.getId()));
        complaints.save(c);
        audit.record("AUDIT_EVENT", "COMPLAINT", c.getCaseCode(), me, "COMPLAINT_FILED", null, "OPEN", r.category(), "SUCCESS");
        return toDto(c);
    }

    @GetMapping("/mine")
    @PreAuthorize(Roles.CITIZEN)
    @Transactional(readOnly = true)
    public List<ComplaintDto> mine(@AuthenticationPrincipal AuthUser me) {
        return complaints.findBySubmittedByOrderByIdDesc(me.id()).stream().map(this::toDto).toList();
    }

    @GetMapping
    @PreAuthorize(Roles.BLO_ERO_ADMIN)
    @Transactional(readOnly = true)
    public List<ComplaintDto> list() { return complaints.findAllByOrderByIdDesc().stream().map(this::toDto).toList(); }

    @PostMapping("/{id}/decide")
    @PreAuthorize(Roles.ERO_ADMIN)
    @Transactional
    public ComplaintDto decide(@PathVariable Long id, @Valid @RequestBody DecisionRequest r, @AuthenticationPrincipal AuthUser me) {
        Complaint c = complaints.findById(id).orElseThrow(() -> ApiException.notFound("Complaint not found"));
        if (List.of("RESOLVED", "REJECTED").contains(c.getStatus())) throw ApiException.conflict("Complaint already closed");
        if (!"UNDER_REVIEW".equals(r.status()) && (r.decision() == null || r.decision().isBlank()))
            throw ApiException.badRequest("A decision note is required to close a complaint");
        String old = c.getStatus();
        c.setStatus(r.status());
        c.setAssignedTo(me.id());
        c.setDecision(r.decision());
        if (!"UNDER_REVIEW".equals(r.status())) c.setDecidedAt(Instant.now());
        complaints.save(c);
        audit.record("AUDIT_EVENT", "COMPLAINT", c.getCaseCode(), me, "COMPLAINT_" + r.status(), old, r.status(), r.decision(), "SUCCESS");
        return toDto(c);
    }
}
