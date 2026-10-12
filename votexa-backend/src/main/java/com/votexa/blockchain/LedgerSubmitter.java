package com.votexa.blockchain;

import com.votexa.audit.AuditEvent;
import com.votexa.audit.AuditEventRepository;
import com.votexa.common.CryptoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Background worker: signs and submits PENDING audit anchors in order.
 * Business operations never fail because the ledger is slow or down; anchors are retried.
 */
@Component
public class LedgerSubmitter {
    private static final Logger log = LoggerFactory.getLogger(LedgerSubmitter.class);
    private static final int MAX_RETRIES = 10;

    private final BlockchainTransactionRepository txs;
    private final AuditEventRepository events;
    private final LedgerClient ledger;
    private final CryptoService crypto;

    public LedgerSubmitter(BlockchainTransactionRepository txs, AuditEventRepository events,
                           LedgerClient ledger, CryptoService crypto) {
        this.txs = txs; this.events = events; this.ledger = ledger; this.crypto = crypto;
    }

    @Scheduled(fixedDelay = 3000, initialDelay = 5000)
    public void flush() {
        List<BlockchainTransaction> pending = txs.findTop20ByStatusOrderByIdAsc("PENDING");
        for (BlockchainTransaction tx : pending) {
            try {
                AuditEvent e = events.findById(tx.getAuditEventId()).orElseThrow();
                String sig = crypto.sign(CryptoService.utf8(tx.getRecordHash()));
                String txId = ledger.submit(new LedgerRecord(e.getEventUuid().toString(), tx.getEventType(),
                        tx.getRecordHash(), tx.getPreviousHash(), e.getCreatedAt().toString(), tx.getAuthorityId(), sig));
                tx.setSignature(sig);
                tx.setFabricTxId(txId);
                tx.setStatus("COMMITTED");
                tx.setCommittedAt(Instant.now());
                txs.save(tx);
            } catch (Exception ex) {
                log.warn("Ledger submit failed for anchor #{}: {}", tx.getId(), ex.getMessage());
                tx.setRetryCount(tx.getRetryCount() + 1);
                if (tx.getRetryCount() >= MAX_RETRIES) tx.setStatus("FAILED");
                txs.save(tx);
                break; // keep ledger order: do not skip ahead
            }
        }
    }
}
