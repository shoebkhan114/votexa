package com.votexa.duplicate;

import com.votexa.auth.AuthUser;
import com.votexa.common.Roles;
import com.votexa.duplicate.DuplicateDtos.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DuplicateController {
    private final DuplicateService service;
    public DuplicateController(DuplicateService service) { this.service = service; }

    @PostMapping("/voters/duplicate-check")
    @PreAuthorize(Roles.BLO_ERO_ADMIN)
    public List<DuplicateMatch> check(@Valid @RequestBody DuplicateCheckRequest r) {
        return service.check(r.fullName(), r.dob(), r.address() == null ? "" : r.address(), null);
    }

    @PostMapping("/duplicates/scan")
    @PreAuthorize(Roles.ERO_ADMIN)
    public ScanResult scan(@AuthenticationPrincipal AuthUser me) { return service.scan(me); }

    @GetMapping("/duplicates")
    @PreAuthorize(Roles.BLO_ERO_ADMIN)
    public List<DuplicateCaseDto> list(@RequestParam(required = false) String status) { return service.list(status); }

    @PostMapping("/duplicates/{id}/review")
    @PreAuthorize(Roles.ERO_ADMIN)
    public DuplicateCaseDto review(@PathVariable Long id, @Valid @RequestBody ReviewRequest r, @AuthenticationPrincipal AuthUser me) {
        return service.review(id, r, me);
    }
}
