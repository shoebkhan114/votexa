package com.votexa.verification;

import com.votexa.auth.AuthUser;
import com.votexa.common.Roles;
import com.votexa.verification.DeathService.DeathCaseDto;
import com.votexa.verification.DeathService.ReviewRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/death-cases")
public class DeathController {
    private final DeathService service;
    public DeathController(DeathService service) { this.service = service; }

    @GetMapping
    @PreAuthorize(Roles.BLO_ERO_ADMIN)
    public List<DeathCaseDto> list() { return service.list(); }

    @PostMapping("/{id}/verify")
    @PreAuthorize(Roles.BLO_ERO_ADMIN)
    public DeathCaseDto verify(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body, @AuthenticationPrincipal AuthUser me) {
        return service.verify(id, body == null ? null : body.get("remarks"), me);
    }

    @PostMapping("/{id}/review")
    @PreAuthorize(Roles.ERO_ADMIN)
    public DeathCaseDto review(@PathVariable Long id, @Valid @RequestBody ReviewRequest r, @AuthenticationPrincipal AuthUser me) {
        return service.review(id, r, me);
    }
}
