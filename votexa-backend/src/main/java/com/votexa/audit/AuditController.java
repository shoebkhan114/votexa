package com.votexa.audit;

import com.votexa.common.Roles;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/audit")
@PreAuthorize(Roles.AUDIT_READ)
public class AuditController {
    public record AuditView(Long id, String eventUuid, String eventType, String entityType, String entityId,
                            String actor, String action, String oldValue, String newValue, String reason,
                            String result, String recordHash, String previousHash, Instant createdAt) {}

    private final AuditEventRepository events;
    public AuditController(AuditEventRepository events) { this.events = events; }

    @GetMapping("/events")
    public List<AuditView> list() {
        return events.findTop200ByOrderByIdDesc().stream().map(e -> new AuditView(e.getId(), e.getEventUuid().toString(),
                e.getEventType(), e.getEntityType(), e.getEntityId(), e.getActorLabel(), e.getAction(), e.getOldValue(),
                e.getNewValue(), e.getReason(), e.getResult(), e.getRecordHash(), e.getPreviousHash(), e.getCreatedAt())).toList();
    }
}
