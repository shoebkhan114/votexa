package com.votexa.voter;

import com.votexa.audit.AuditService;
import com.votexa.auth.AuthUser;
import com.votexa.common.ApiException;
import com.votexa.duplicate.DuplicateDtos.DuplicateMatch;
import com.votexa.duplicate.DuplicateService;
import com.votexa.election.Constituency;
import com.votexa.election.ConstituencyRepository;
import com.votexa.election.PollingStation;
import com.votexa.election.PollingStationRepository;
import com.votexa.voter.VoterDtos.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class VoterService {
    private final VoterRepository voters;
    private final AddressRepository addresses;
    private final StatusChangeRepository changes;
    private final ConstituencyRepository constituencies;
    private final PollingStationRepository stations;
    private final StatusService statusService;
    private final DuplicateService duplicates;
    private final AuditService audit;

    public VoterService(VoterRepository voters, AddressRepository addresses, StatusChangeRepository changes,
                        ConstituencyRepository constituencies, PollingStationRepository stations,
                        StatusService statusService, DuplicateService duplicates, AuditService audit) {
        this.voters = voters; this.addresses = addresses; this.changes = changes; this.constituencies = constituencies;
        this.stations = stations; this.statusService = statusService; this.duplicates = duplicates; this.audit = audit;
    }

    public VoterDto toDto(Voter v) {
        AddressDto a = addresses.findByVoterIdAndCurrentTrue(v.getId())
                .map(x -> new AddressDto(x.getLine1(), x.getLocality(), x.getDistrict(), x.getState(), x.getPincode()))
                .orElse(null);
        Constituency c = v.getConstituency();
        PollingStation ps = v.getPollingStation();
        return new VoterDto(v.getVoterCode(), v.getFullName(), v.getDob(), v.getGender(), v.getStatus(),
                c.getCode(), c.getName(), c.getDistrict(), c.getState(),
                ps == null ? null : ps.getCode(), ps == null ? null : ps.getName(),
                v.getRegistrationDate() == null ? LocalDate.now() : v.getRegistrationDate(), a);
    }

    public Voter find(String code) {
        return voters.findByVoterCode(code.trim()).orElseThrow(() -> ApiException.notFound("Voter not found: " + code));
    }

    @Transactional(readOnly = true)
    public List<VoterDto> search(String q, String status, int limit) {
        var page = voters.search(q == null ? "" : q.trim(), status == null ? "" : status.trim(),
                PageRequest.of(0, Math.min(Math.max(limit, 1), 200), Sort.by("id")));
        return page.getContent().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public VoterDto get(String code) { return toDto(find(code)); }

    @Transactional(readOnly = true)
    public VoterDto mine(AuthUser me) {
        return toDto(voters.findByCitizenUserId(me.id())
                .orElseThrow(() -> ApiException.notFound("No voter record is linked to your account")));
    }

    @Transactional(readOnly = true)
    public List<StatusChangeDto> history(String code) {
        Voter v = find(code);
        return changes.findByVoterIdOrderByIdDesc(v.getId()).stream()
                .map(s -> new StatusChangeDto(s.getOldStatus(), s.getNewStatus(), s.getReason(), s.getSourceRef(), s.getCreatedAt())).toList();
    }

    @Transactional
    public CreateVoterResponse create(CreateVoterRequest r, AuthUser actor) {
        if (r.dob().isAfter(LocalDate.now().minusYears(18)))
            throw ApiException.badRequest("Voter must be at least 18 years old");
        Constituency c = constituencies.findByCode(r.constituencyCode())
                .orElseThrow(() -> ApiException.notFound("Constituency not found: " + r.constituencyCode()));

        Voter v = new Voter();
        v.setVoterCode("TMP-" + UUID.randomUUID().toString().substring(0, 12));
        v.setFullName(r.fullName().trim());
        v.setDob(r.dob());
        v.setGender(r.gender());
        v.setStatus("PENDING");
        v.setConstituency(c);
        voters.save(v);
        v.setVoterCode(String.format("VTX%07d", v.getId()));
        StatusService.rehash(v);
        voters.save(v);

        Address a = new Address();
        a.setVoterId(v.getId());
        a.setLine1(r.line1().trim());
        a.setLocality(r.locality().trim());
        a.setDistrict(c.getDistrict());
        a.setState(c.getState());
        a.setPincode(r.pincode());
        addresses.save(a);

        List<DuplicateMatch> dups = duplicates.check(v.getFullName(), v.getDob(), a.getLine1() + " " + a.getLocality(), v.getId());
        audit.record("AUDIT_EVENT", "VOTER", v.getVoterCode(), actor, "VOTER_REGISTERED", null, "PENDING",
                dups.isEmpty() ? "New registration" : "New registration; possible duplicates flagged", "SUCCESS");
        return new CreateVoterResponse(toDto(v), dups);
    }

    @Transactional
    public VerifyResult verify(String code, AuthUser actor) {
        String c = code.trim();
        Voter v = voters.findByVoterCode(c).orElse(null);
        List<CheckDto> checks = new ArrayList<>();
        checks.add(new CheckDto("Voter record found", v != null));
        if (v == null) {
            audit.record("VOTER_VERIFICATION", "VOTER", c, actor, "VOTER_VERIFIED", null, null, "Record not found", "FAILURE");
            return new VerifyResult(c, false, false, checks, "Voter record not found");
        }
        boolean active = "ACTIVE".equals(v.getStatus());
        boolean hasStation = v.getPollingStation() != null;
        checks.add(new CheckDto("Status is ACTIVE", active));
        checks.add(new CheckDto("Constituency assigned", v.getConstituency() != null));
        checks.add(new CheckDto("Polling station assigned", hasStation));
        boolean eligible = active && hasStation;
        String msg = eligible ? "Voter found, ACTIVE, polling station assigned: eligible"
                : !active ? "Record is " + v.getStatus() + ": verification required" : "No polling station assigned";
        audit.record("VOTER_VERIFICATION", "VOTER", c, actor, "VOTER_VERIFIED", v.getStatus(), eligible ? "ELIGIBLE" : "NOT_ELIGIBLE", msg,
                eligible ? "SUCCESS" : "FAILURE");
        return new VerifyResult(c, true, eligible, checks, msg);
    }

    @Transactional
    public VoterDto changeStatus(String code, StatusChangeRequest r, AuthUser actor) {
        Voter v = find(code);
        if (r.pollingStationCode() != null && !r.pollingStationCode().isBlank()) {
            PollingStation ps = stations.findByCode(r.pollingStationCode())
                    .orElseThrow(() -> ApiException.notFound("Polling station not found: " + r.pollingStationCode()));
            v.setPollingStation(ps);
            v.setConstituency(ps.getConstituency());
        }
        statusService.change(v, r.newStatus(), r.reason(), actor, null);
        return toDto(v);
    }
}
