package com.votexa.blockchain;

import com.votexa.audit.AuditEvent;
import com.votexa.audit.AuditEventRepository;
import com.votexa.audit.AuditService;
import com.votexa.common.CryptoService;
import com.votexa.common.Hashing;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/** Re-computes the audit hash chain and compares it against the ledger copy. */
@Service
public class ChainVerificationService {
    public record Report(boolean verified, int eventsChecked, int ledgerRecordsChecked, long ledgerRecordCount,
                         List<String> violations, Instant checkedAt, String ledgerMode) {}

    private final AuditEventRepository events;
    private final BlockchainTransactionRepository txs;
    private final LedgerClient ledger;
    private final CryptoService crypto;

    public ChainVerificationService(AuditEventRepository events, BlockchainTransactionRepository txs,
                                    LedgerClient ledger, CryptoService crypto) {
        this.events = events; this.txs = txs; this.ledger = ledger; this.crypto = crypto;
    }

    @Transactional
    public Report verifyAll() {
        List<AuditEvent> all = events.findAllByOrderByIdAsc();
        Map<Long, BlockchainTransaction> byEvent = txs.findAll().stream()
                .collect(Collectors.toMap(BlockchainTransaction::getAuditEventId, t -> t, (a, b) -> a));
        Set<Long> bad = new HashSet<>();
        List<String> violations = new ArrayList<>();
        String prev = Hashing.ZERO;
        int ledgerChecked = 0;
        long committed = 0;

        for (AuditEvent e : all) {
            if (!AuditService.computeHash(e).equals(e.getRecordHash())) {
                violations.add("Event #" + e.getId() + ": content does not match its recorded hash (possible tampering)");
                bad.add(e.getId());
            }
            if (!prev.equals(e.getPreviousHash())) {
                violations.add("Event #" + e.getId() + ": hash chain broken (previous hash mismatch)");
                bad.add(e.getId());
            }
            prev = e.getRecordHash();

            BlockchainTransaction t = byEvent.get(e.getId());
            if (t != null && "COMMITTED".equals(t.getStatus())) {
                committed++;
                ledgerChecked++;
                if (!t.getRecordHash().equals(e.getRecordHash())) {
                    violations.add("Event #" + e.getId() + ": anchor row differs from audit event hash");
                    bad.add(e.getId());
                }
                if (t.getSignature() == null || !crypto.verify(CryptoService.utf8(t.getRecordHash()), t.getSignature())) {
                    violations.add("Event #" + e.getId() + ": authority signature invalid");
                    bad.add(e.getId());
                }
                try {
                    Optional<String> lh = ledger.fetchRecordHash(e.getEventUuid().toString());
                    if (lh.isEmpty()) {
                        violations.add("Event #" + e.getId() + ": not found on the ledger");
                        bad.add(e.getId());
                    } else if (!lh.get().equals(e.getRecordHash())) {
                        violations.add("Event #" + e.getId() + ": ledger hash differs from database hash");
                        bad.add(e.getId());
                    }
                } catch (Exception ex) {
                    violations.add("Event #" + e.getId() + ": ledger unreachable, cannot verify (" + ex.getMessage() + ")");
                }
            }
        }

        long count = -1;
        try { count = ledger.recordCount(); } catch (Exception ignored) { }
        if (count >= 0 && count > committed)
            violations.add("Ledger holds " + count + " records but the database has only " + committed
                    + " committed audit events (events removed from the database?)");

        for (BlockchainTransaction t : byEvent.values()) {
            if ("COMMITTED".equals(t.getStatus())) {
                t.setIntegrityStatus(bad.contains(t.getAuditEventId()) ? "VIOLATION" : "VERIFIED");
                t.setLastVerifiedAt(Instant.now());
                txs.save(t);
            }
        }
        return new Report(violations.isEmpty(), all.size(), ledgerChecked, count, violations, Instant.now(), ledger.mode());
    }
}
