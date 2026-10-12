package com.votexa.anomaly;

import com.votexa.auth.AuthUser;
import com.votexa.common.ApiException;
import com.votexa.common.Roles;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/anomalies")
public class AnomalyController {
    private final AnomalyAlertRepository alerts;
    public AnomalyController(AnomalyAlertRepository alerts) { this.alerts = alerts; }

    @GetMapping
    @PreAuthorize(Roles.POLLING_READ)
    public List<AnomalyAlert> list() { return alerts.findTop100ByOrderByIdDesc(); }

    @PostMapping("/{id}/{action}")
    @PreAuthorize(Roles.AUDIT_READ)
    public AnomalyAlert update(@PathVariable Long id, @PathVariable String action, @AuthenticationPrincipal AuthUser me) {
        AnomalyAlert a = alerts.findById(id).orElseThrow(() -> ApiException.notFound("Alert not found"));
        switch (action) {
            case "acknowledge" -> a.setStatus("ACKNOWLEDGED");
            case "resolve" -> { a.setStatus("RESOLVED"); a.setResolvedBy(me.id()); a.setResolvedAt(Instant.now()); }
            default -> throw ApiException.badRequest("Unknown action: " + action);
        }
        return alerts.save(a);
    }
}
