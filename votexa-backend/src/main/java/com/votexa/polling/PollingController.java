package com.votexa.polling;

import com.votexa.auth.AuthUser;
import com.votexa.common.Roles;
import com.votexa.polling.PollingDtos.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/polling")
public class PollingController {
    private final PollingService service;
    public PollingController(PollingService service) { this.service = service; }

    @PostMapping("/validate")
    @PreAuthorize(Roles.POLLING)
    public PollingResult validate(@Valid @RequestBody PollingRequest r, @AuthenticationPrincipal AuthUser me) {
        return service.validate(r, me);
    }

    @PostMapping("/authorize")
    @PreAuthorize(Roles.POLLING)
    public PollingResult authorize(@Valid @RequestBody PollingRequest r, @AuthenticationPrincipal AuthUser me) {
        return service.authorize(r, me);
    }

    @PostMapping("/sync")
    @PreAuthorize(Roles.POLLING)
    public SyncResult sync(@Valid @RequestBody SyncRequest r, @AuthenticationPrincipal AuthUser me) {
        return service.sync(r, me);
    }

    @GetMapping("/sessions")
    @PreAuthorize(Roles.POLLING_READ)
    public List<SessionStats> sessions(@RequestParam(required = false) Long electionId) { return service.listSessions(electionId); }

    @GetMapping("/sessions/{id}/dashboard")
    @PreAuthorize(Roles.POLLING_READ)
    public SessionStats dashboard(@PathVariable Long id) { return service.dashboard(id); }

    @GetMapping("/sessions/{id}/attempts")
    @PreAuthorize(Roles.POLLING_READ)
    public List<AttemptView> attempts(@PathVariable Long id) { return service.attempts(id); }

    @PostMapping("/sessions/{id}/open")
    @PreAuthorize(Roles.POLLING)
    public SessionStats open(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) { return service.open(id, me); }

    @PostMapping("/sessions/{id}/close")
    @PreAuthorize(Roles.POLLING)
    public Reconciliation close(@PathVariable Long id, @Valid @RequestBody CloseRequest r, @AuthenticationPrincipal AuthUser me) {
        return service.close(id, r.recordedCount(), me);
    }

    @GetMapping("/sessions/{id}/reconciliation")
    @PreAuthorize(Roles.POLLING_READ)
    public Reconciliation reconciliation(@PathVariable Long id) { return service.reconciliation(id); }

    @GetMapping("/reconciliation")
    @PreAuthorize(Roles.POLLING_READ)
    public List<Reconciliation> reconciliationAll(@RequestParam(required = false) Long electionId) { return service.reconciliationAll(electionId); }
}
