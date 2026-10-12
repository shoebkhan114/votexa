package com.votexa.address;

import com.votexa.address.AddressDtos.*;
import com.votexa.audit.AuditService;
import com.votexa.auth.AuthUser;
import com.votexa.common.ApiException;
import com.votexa.election.Constituency;
import com.votexa.election.ConstituencyRepository;
import com.votexa.election.PollingStation;
import com.votexa.election.PollingStationRepository;
import com.votexa.voter.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Address / constituency migration workflow:
 * request -> verification (BLO/ERO) -> approval (ERO) -> old area SHIFTED -> new area ACTIVE + new station -> audit.
 * Nothing is moved automatically, including requests raised by the mock Aadhaar event service.
 */
@Service
public class AddressChangeService {
    private static final List<String> OPEN = List.of("PENDING", "UNDER_VERIFICATION", "VERIFIED");

    private final AddressChangeRequestRepository reqs;
    private final VoterRepository voters;
    private final AddressRepository addresses;
    private final PollingStationRepository stations;
    private final ConstituencyRepository constituencies;
    private final StatusService statusService;
    private final AuditService audit;

    public AddressChangeService(AddressChangeRequestRepository reqs, VoterRepository voters, AddressRepository addresses,
                                PollingStationRepository stations, ConstituencyRepository constituencies,
                                StatusService statusService, AuditService audit) {
        this.reqs = reqs; this.voters = voters; this.addresses = addresses; this.stations = stations;
        this.constituencies = constituencies; this.statusService = statusService; this.audit = audit;
    }

    private AddressChangeDto toDto(AddressChangeRequest r) {
        Voter v = voters.findById(r.getVoterId()).orElseThrow();
        String cc = r.getNewConstituencyId() == null ? null
                : constituencies.findById(r.getNewConstituencyId()).map(Constituency::getCode).orElse(null);
        String sc = r.getNewPollingStationId() == null ? null
                : stations.findById(r.getNewPollingStationId()).map(PollingStation::getCode).orElse(null);
        return new AddressChangeDto(r.getId(), r.getRequestCode(), v.getVoterCode(), v.getFullName(), r.getSource(),
                r.getNewLine1(), r.getNewLocality(), r.getNewDistrict(), r.getNewState(), r.getNewPincode(), cc, sc,
                r.getStatus(), r.getReason(), r.getDecisionRemarks(), r.getCreatedAt());
    }

    private AddressChangeRequest find(Long id) {
        return reqs.findById(id).orElseThrow(() -> ApiException.notFound("Address change request not found"));
    }

    @Transactional
    public AddressChangeDto create(CreateAddressChangeRequest r, String source, AuthUser actor) {
        Voter v;
        if ("CITIZEN".equals(actor.role())) {
            v = voters.findByCitizenUserId(actor.id())
                    .orElseThrow(() -> ApiException.notFound("No voter record is linked to your account"));
        } else {
            if (r.voterCode() == null || r.voterCode().isBlank()) throw ApiException.badRequest("voterCode is required");
            v = voters.findByVoterCode(r.voterCode().trim()).orElseThrow(() -> ApiException.notFound("Voter not found"));
        }
        if (reqs.existsByVoterIdAndStatusIn(v.getId(), OPEN))
            throw ApiException.conflict("This voter already has an open address change request");

        AddressChangeRequest a = new AddressChangeRequest();
        a.setRequestCode("TMP-" + UUID.randomUUID().toString().substring(0, 12));
        a.setVoterId(v.getId());
        a.setSource(source);
        a.setNewLine1(r.line1().trim());
        a.setNewLocality(r.locality().trim());
        a.setNewDistrict(blank(r.district()) ? v.getConstituency().getDistrict() : r.district().trim());
        a.setNewState(blank(r.state()) ? v.getConstituency().getState() : r.state().trim());
        a.setNewPincode(r.pincode());
        a.setReason(r.reason());
        if (!blank(r.stationCode())) {
            PollingStation ps = stations.findByCode(r.stationCode().trim())
                    .orElseThrow(() -> ApiException.notFound("Polling station not found: " + r.stationCode()));
            a.setNewPollingStationId(ps.getId());
            a.setNewConstituencyId(ps.getConstituency().getId());
        }
        reqs.save(a);
        a.setRequestCode(String.format("ACR-%04d", a.getId()));
        reqs.save(a);
        audit.record("ADDRESS_CHANGE", "ADDRESS_CHANGE_REQUEST", a.getRequestCode(), actor, "ADDRESS_CHANGE_REQUESTED",
                null, "PENDING", source + (blank(r.reason()) ? "" : ": " + r.reason()), "SUCCESS");
        return toDto(a);
    }

    private static boolean blank(String s) { return s == null || s.isBlank(); }

    @Transactional(readOnly = true)
    public List<AddressChangeDto> list(String status) {
        List<AddressChangeRequest> l = blank(status) ? reqs.findAllByOrderByIdDesc() : reqs.findByStatusOrderByIdDesc(status);
        return l.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<AddressChangeDto> mine(AuthUser me) {
        Voter v = voters.findByCitizenUserId(me.id()).orElseThrow(() -> ApiException.notFound("No voter record is linked to your account"));
        return reqs.findByVoterIdOrderByIdDesc(v.getId()).stream().map(this::toDto).toList();
    }

    @Transactional
    public AddressChangeDto verify(Long id, DecisionRequest d, AuthUser actor) {
        AddressChangeRequest r = find(id);
        if (!List.of("PENDING", "UNDER_VERIFICATION").contains(r.getStatus()))
            throw ApiException.conflict("Only PENDING requests can be verified (current: " + r.getStatus() + ")");
        r.setStatus("VERIFIED");
        r.setVerifiedBy(actor.id());
        r.setVerifiedAt(Instant.now());
        if (!blank(d.remarks())) r.setDecisionRemarks(d.remarks());
        reqs.save(r);
        audit.record("VOTER_VERIFICATION", "ADDRESS_CHANGE_REQUEST", r.getRequestCode(), actor, "ADDRESS_VERIFIED",
                "PENDING", "VERIFIED", d.remarks(), "SUCCESS");
        return toDto(r);
    }

    @Transactional
    public AddressChangeDto reject(Long id, DecisionRequest d, AuthUser actor) {
        AddressChangeRequest r = find(id);
        if (!OPEN.contains(r.getStatus())) throw ApiException.conflict("Request already closed (" + r.getStatus() + ")");
        if (blank(d.remarks())) throw ApiException.badRequest("Remarks are required to reject a request");
        String old = r.getStatus();
        r.setStatus("REJECTED");
        r.setDecidedBy(actor.id());
        r.setDecidedAt(Instant.now());
        r.setDecisionRemarks(d.remarks());
        reqs.save(r);
        audit.record("ADDRESS_CHANGE", "ADDRESS_CHANGE_REQUEST", r.getRequestCode(), actor, "ADDRESS_CHANGE_REJECTED",
                old, "REJECTED", d.remarks(), "SUCCESS");
        return toDto(r);
    }

    @Transactional
    public AddressChangeDto approve(Long id, DecisionRequest d, AuthUser actor) {
        AddressChangeRequest r = find(id);
        if (!"VERIFIED".equals(r.getStatus()))
            throw ApiException.conflict("Request must be VERIFIED before approval (current: " + r.getStatus() + ")");
        Voter v = voters.findById(r.getVoterId()).orElseThrow();

        PollingStation target = null;
        if (!blank(d.stationCode())) {
            target = stations.findByCode(d.stationCode().trim())
                    .orElseThrow(() -> ApiException.notFound("Polling station not found: " + d.stationCode()));
        } else if (r.getNewPollingStationId() != null) {
            target = stations.findById(r.getNewPollingStationId()).orElseThrow();
        }
        if (target == null && v.getPollingStation() == null)
            throw ApiException.badRequest("A polling station must be chosen to approve this request");

        String oldCons = v.getConstituency().getCode();
        String oldStation = v.getPollingStation() == null ? "-" : v.getPollingStation().getCode();

        // close old current address first (partial unique index allows one current address)
        addresses.findByVoterIdAndCurrentTrue(v.getId()).ifPresent(a -> {
            a.setCurrent(false);
            a.setValidTo(LocalDate.now());
            addresses.saveAndFlush(a);
        });
        Address n = new Address();
        n.setVoterId(v.getId());
        n.setLine1(r.getNewLine1());
        n.setLocality(r.getNewLocality());
        n.setDistrict(r.getNewDistrict());
        n.setState(r.getNewState());
        n.setPincode(r.getNewPincode());
        n.setCurrent(true);
        n.setValidFrom(LocalDate.now());
        addresses.save(n);

        boolean consChanged = target != null && !target.getConstituency().getId().equals(v.getConstituency().getId());
        String oldStatus = v.getStatus();
        if (target != null) { v.setConstituency(target.getConstituency()); v.setPollingStation(target); }
        String newCons = v.getConstituency().getCode();
        String newStation = v.getPollingStation().getCode();

        if (consChanged && "ACTIVE".equals(oldStatus)) {
            // old area: SHIFTED, new area: ACTIVE (both recorded in status history)
            statusService.change(v, "SHIFTED", "Moved out of constituency " + oldCons, actor, r.getRequestCode());
            statusService.change(v, "ACTIVE", "Assigned to " + newCons + " / " + newStation, actor, r.getRequestCode());
        } else if (!"ACTIVE".equals(oldStatus) && List.of("SHIFTED", "PENDING", "UNDER_VERIFICATION").contains(oldStatus)) {
            statusService.change(v, "ACTIVE", "Address change approved: " + newCons + " / " + newStation, actor, r.getRequestCode());
        } else {
            voters.save(v);
        }

        r.setStatus("APPROVED");
        r.setDecidedBy(actor.id());
        r.setDecidedAt(Instant.now());
        r.setDecisionRemarks(blank(d.remarks()) ? r.getDecisionRemarks() : d.remarks());
        reqs.save(r);
        audit.record("ADDRESS_CHANGE", "VOTER", v.getVoterCode(), actor, "ADDRESS_CHANGE_APPROVED",
                oldCons + "/" + oldStation, newCons + "/" + newStation, d.remarks(), "SUCCESS");
        return toDto(r);
    }
}
