package com.votexa.voter;

import com.votexa.auth.AuthUser;
import com.votexa.common.Roles;
import com.votexa.voter.VoterDtos.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/voters")
public class VoterController {
    private final VoterService service;
    public VoterController(VoterService service) { this.service = service; }

    @GetMapping
    @PreAuthorize(Roles.STAFF)
    public List<VoterDto> search(@RequestParam(defaultValue = "") String q,
                                 @RequestParam(defaultValue = "") String status,
                                 @RequestParam(defaultValue = "100") int limit) {
        return service.search(q, status, limit);
    }

    @GetMapping("/me")
    @PreAuthorize(Roles.CITIZEN)
    public VoterDto me(@AuthenticationPrincipal AuthUser me) { return service.mine(me); }

    @GetMapping("/{code}")
    @PreAuthorize(Roles.STAFF)
    public VoterDto get(@PathVariable String code) { return service.get(code); }

    @GetMapping("/{code}/history")
    @PreAuthorize(Roles.STAFF)
    public List<StatusChangeDto> history(@PathVariable String code) { return service.history(code); }

    @PostMapping
    @PreAuthorize(Roles.ERO_ADMIN)
    public CreateVoterResponse create(@Valid @RequestBody CreateVoterRequest r, @AuthenticationPrincipal AuthUser me) {
        return service.create(r, me);
    }

    @PostMapping("/verify")
    @PreAuthorize(Roles.VERIFIERS)
    public VerifyResult verify(@Valid @RequestBody VerifyRequest r, @AuthenticationPrincipal AuthUser me) {
        return service.verify(r.voterCode(), me);
    }

    @PostMapping("/{code}/status")
    @PreAuthorize(Roles.ERO_ADMIN)
    public VoterDto status(@PathVariable String code, @Valid @RequestBody StatusChangeRequest r,
                           @AuthenticationPrincipal AuthUser me) {
        return service.changeStatus(code, r, me);
    }
}
