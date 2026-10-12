package com.votexa.duplicate;

import com.votexa.audit.AuditService;
import com.votexa.auth.AuthUser;
import com.votexa.common.ApiException;
import com.votexa.duplicate.DuplicateDtos.*;
import com.votexa.voter.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DuplicateService {
    private static final Set<String> SCAN_STATUSES = Set.of("ACTIVE", "PENDING", "UNDER_VERIFICATION", "DUPLICATE_REVIEW");

    private final VoterRepository voters;
    private final AddressRepository addresses;
    private final DuplicateCaseRepository cases;
    private final StatusService statusService;
    private final AuditService audit;
    private final double threshold;

    public DuplicateService(VoterRepository voters, AddressRepository addresses, DuplicateCaseRepository cases,
                            StatusService statusService, AuditService audit,
                            @Value("${votexa.duplicate.threshold}") double threshold) {
        this.voters = voters; this.addresses = addresses; this.cases = cases;
        this.statusService = statusService; this.audit = audit; this.threshold = threshold;
    }

    private String addressOf(Long voterId) {
        return addresses.findByVoterIdAndCurrentTrue(voterId).map(a -> a.getLine1() + " " + a.getLocality()).orElse("");
    }

    /** Candidate lookup for a (possibly new) record. Same date of birth, name/address similarity scored in Java. */
    @Transactional(readOnly = true)
    public List<DuplicateMatch> check(String fullName, LocalDate dob, String address, Long excludeVoterId) {
        List<DuplicateMatch> out = new ArrayList<>();
        for (Voter v : voters.findByDob(dob)) {
            if (v.getId().equals(excludeVoterId)) continue;
            double s = Similarity.score(fullName, dob, address, v.getFullName(), v.getDob(), addressOf(v.getId()));
            if (s >= threshold) out.add(new DuplicateMatch(v.getVoterCode(), v.getFullName(), v.getDob(), s));
        }
        out.sort(Comparator.comparingDouble(DuplicateMatch::score).reversed());
        return out;
    }

    @Transactional
    public ScanResult scan(AuthUser actor) {
        List<Voter> pool = voters.findAll().stream().filter(v -> SCAN_STATUSES.contains(v.getStatus())).toList();
        Map<Long, String> addr = addresses.findByCurrentTrue().stream()
                .collect(Collectors.toMap(a -> a.getVoterId(), a -> a.getLine1() + " " + a.getLocality(), (x, y) -> x));
        int pairs = 0, created = 0;
        for (List<Voter> group : pool.stream().collect(Collectors.groupingBy(Voter::getDob)).values()) {
            if (group.size() < 2) continue;
            List<Voter> g = group.stream().sorted(Comparator.comparing(Voter::getId)).toList();
            for (int i = 0; i < g.size(); i++) {
                for (int j = i + 1; j < g.size(); j++) {
                    Voter a = g.get(i), b = g.get(j);
                    pairs++;
                    String aa = addr.getOrDefault(a.getId(), ""), ba = addr.getOrDefault(b.getId(), "");
                    double s = Similarity.score(a.getFullName(), a.getDob(), aa, b.getFullName(), b.getDob(), ba);
                    if (s >= threshold && !cases.existsByVoterAIdAndVoterBId(a.getId(), b.getId())) {
                        DuplicateCase c = new DuplicateCase();
                        c.setCaseCode("TMP-" + UUID.randomUUID().toString().substring(0, 12));
                        c.setVoterAId(a.getId());
                        c.setVoterBId(b.getId());
                        c.setSimilarityScore(BigDecimal.valueOf(s));
                        c.setMatchedAttributes(Similarity.matched(a.getFullName(), a.getDob(), aa, b.getFullName(), b.getDob(), ba));
                        cases.save(c);
                        c.setCaseCode(String.format("DUP-%04d", c.getId()));
                        cases.save(c);
                        created++;
                    }
                }
            }
        }
        audit.record("DUPLICATE_REVIEW", "DUPLICATE_SCAN", "SCAN", actor, "DUPLICATE_SCAN_RUN", null,
                created + " new case(s)", "Pairs checked: " + pairs, "SUCCESS");
        return new ScanResult(pairs, created);
    }

    private VoterRef ref(Long id) {
        Voter v = voters.findById(id).orElseThrow();
        return new VoterRef(v.getVoterCode(), v.getFullName(), v.getDob(), v.getStatus());
    }

    private DuplicateCaseDto toDto(DuplicateCase c) {
        return new DuplicateCaseDto(c.getId(), c.getCaseCode(), ref(c.getVoterAId()), ref(c.getVoterBId()),
                c.getSimilarityScore().doubleValue(), c.getMatchedAttributes(), c.getStatus(), c.getRemarks(), c.getDetectedAt());
    }

    @Transactional(readOnly = true)
    public List<DuplicateCaseDto> list(String status) {
        List<DuplicateCase> l = (status == null || status.isBlank()) ? cases.findAllByOrderByIdDesc()
                : cases.findByStatusOrderByIdDesc(status);
        return l.stream().map(this::toDto).toList();
    }

    @Transactional
    public DuplicateCaseDto review(Long id, ReviewRequest req, AuthUser actor) {
        DuplicateCase c = cases.findById(id).orElseThrow(() -> ApiException.notFound("Duplicate case not found"));
        if (!"OPEN".equals(c.getStatus())) throw ApiException.conflict("Case already reviewed");
        Voter a = voters.findById(c.getVoterAId()).orElseThrow();
        Voter b = voters.findById(c.getVoterBId()).orElseThrow();
        boolean confirm = "CONFIRM".equals(req.decision());
        if (confirm) {
            c.setStatus("CONFIRMED");
            statusService.change(b, "INACTIVE", "Confirmed duplicate of " + a.getVoterCode() + " (" + c.getCaseCode() + ")", actor, c.getCaseCode());
        } else {
            c.setStatus("REJECTED");
            for (Voter v : List.of(a, b)) {
                if ("DUPLICATE_REVIEW".equals(v.getStatus()))
                    statusService.change(v, v.getPollingStation() != null ? "ACTIVE" : "PENDING",
                            "Duplicate match rejected (" + c.getCaseCode() + ")", actor, c.getCaseCode());
            }
        }
        c.setReviewedBy(actor.id());
        c.setReviewedAt(Instant.now());
        c.setRemarks(req.remarks());
        cases.save(c);
        audit.record("DUPLICATE_REVIEW", "DUPLICATE_CASE", c.getCaseCode(), actor,
                confirm ? "DUPLICATE_CONFIRMED" : "DUPLICATE_REJECTED", "OPEN", c.getStatus(), req.remarks(), "SUCCESS");
        return toDto(c);
    }
}
