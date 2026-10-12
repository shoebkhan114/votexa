package com.votexa;

import com.votexa.audit.AuditEvent;
import com.votexa.audit.AuditService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuditHashTest {
    private AuditEvent sample() {
        AuditEvent e = new AuditEvent();
        e.setEventUuid(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        e.setEventType("STATUS_CHANGE");
        e.setEntityType("VOTER");
        e.setEntityId("VTX0000001");
        e.setActorLabel("ERO102");
        e.setAction("STATUS_CHANGED");
        e.setOldValue("ACTIVE");
        e.setNewValue("INACTIVE");
        e.setReason("Shifted");
        e.setResult("SUCCESS");
        e.setCreatedAt(Instant.parse("2026-10-08T10:00:00.123456Z"));
        e.setPreviousHash("0".repeat(64));
        return e;
    }

    @Test
    void hashIsDeterministic() {
        assertEquals(AuditService.computeHash(sample()), AuditService.computeHash(sample()));
        assertEquals(64, AuditService.computeHash(sample()).length());
    }

    @Test
    void anyFieldChangeChangesTheHash() {
        AuditEvent a = sample();
        AuditEvent b = sample();
        b.setNewValue("ACTIVE");
        assertNotEquals(AuditService.computeHash(a), AuditService.computeHash(b));
        AuditEvent c = sample();
        c.setPreviousHash("1".repeat(64));
        assertNotEquals(AuditService.computeHash(a), AuditService.computeHash(c));
    }
}
