package com.votexa.audit;

import com.votexa.auth.AuthUser;
import com.votexa.blockchain.BlockchainTransaction;
import com.votexa.blockchain.BlockchainTransactionRepository;
import com.votexa.common.Hashing;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Creates hash-chained audit events and queues them for the blockchain ledger.
 * Only hashes + metadata leave the database; personal data and vote choice never go on-chain.
 */
@Service
public class AuditService {
    private final AuditEventRepository events;
    private final BlockchainTransactionRepository txs;
    private final JdbcTemplate jdbc;

    public AuditService(AuditEventRepository events, BlockchainTransactionRepository txs, JdbcTemplate jdbc) {
        this.events = events; this.txs = txs; this.jdbc = jdbc;
    }

    private static String nz(String s) { return s == null ? "" : s; }
    private static String cut(String s, int n) { return s == null ? null : (s.length() > n ? s.substring(0, n) : s); }

    public static String computeHash(AuditEvent e) {
        String raw = String.join("|", e.getEventUuid().toString(), e.getEventType(), e.getEntityType(), e.getEntityId(),
                nz(e.getActorLabel()), e.getAction(), nz(e.getOldValue()), nz(e.getNewValue()), nz(e.getReason()),
                e.getResult(), e.getCreatedAt().toString(), e.getPreviousHash());
        return Hashing.sha256Hex(raw);
    }

    @Transactional
    public AuditEvent record(String eventType, String entityType, String entityId, AuthUser actor, String action,
                             String oldValue, String newValue, String reason, String result) {
        // serialise chain creation across concurrent transactions
        jdbc.execute("SELECT pg_advisory_xact_lock(7001)");
        String prev = events.findTopByOrderByIdDesc().map(AuditEvent::getRecordHash).orElse(Hashing.ZERO);

        AuditEvent e = new AuditEvent();
        e.setEventUuid(UUID.randomUUID());
        e.setEventType(eventType);
        e.setEntityType(cut(entityType, 40));
        e.setEntityId(cut(entityId, 64));
        e.setActorUserId(actor == null ? null : actor.id());
        e.setActorLabel(actor == null ? "SYSTEM" : cut(actor.username().toUpperCase(), 60));
        e.setAction(cut(action, 60));
        e.setOldValue(cut(oldValue, 200));
        e.setNewValue(cut(newValue, 200));
        e.setReason(cut(reason, 300));
        e.setResult(result == null ? "SUCCESS" : result);
        e.setCreatedAt(Instant.now().truncatedTo(ChronoUnit.MICROS));
        e.setPreviousHash(prev);
        e.setRecordHash(computeHash(e));
        events.save(e);

        BlockchainTransaction tx = new BlockchainTransaction();
        tx.setAuditEventId(e.getId());
        tx.setEventType(e.getEventType());
        tx.setRecordHash(e.getRecordHash());
        tx.setPreviousHash(e.getPreviousHash());
        tx.setAuthorityId(e.getActorLabel());
        tx.setStatus("PENDING");
        tx.setIntegrityStatus("UNVERIFIED");
        txs.save(tx);
        return e;
    }
}
