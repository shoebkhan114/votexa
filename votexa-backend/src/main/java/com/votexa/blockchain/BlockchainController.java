package com.votexa.blockchain;

import com.votexa.audit.AuditEvent;
import com.votexa.audit.AuditEventRepository;
import com.votexa.common.CryptoService;
import com.votexa.common.Roles;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/blockchain")
@PreAuthorize(Roles.AUDIT_READ)
public class BlockchainController {
    public record EventView(Long id, String eventUuid, String eventType, String entityType, String action,
                            String recordHash, String previousHash, Instant timestamp, String authority,
                            String txId, String status, String integrity) {}

    private final BlockchainTransactionRepository txs;
    private final AuditEventRepository events;
    private final ChainVerificationService verifier;
    private final LedgerClient ledger;
    private final CryptoService crypto;

    public BlockchainController(BlockchainTransactionRepository txs, AuditEventRepository events,
                                ChainVerificationService verifier, LedgerClient ledger, CryptoService crypto) {
        this.txs = txs; this.events = events; this.verifier = verifier; this.ledger = ledger; this.crypto = crypto;
    }

    @GetMapping("/events")
    public List<EventView> list() {
        Map<Long, AuditEvent> ev = events.findTop200ByOrderByIdDesc().stream()
                .collect(Collectors.toMap(AuditEvent::getId, Function.identity()));
        return txs.findTop200ByOrderByIdDesc().stream().filter(t -> ev.containsKey(t.getAuditEventId())).map(t -> {
            AuditEvent e = ev.get(t.getAuditEventId());
            return new EventView(t.getId(), e.getEventUuid().toString(), t.getEventType(), e.getEntityType(), e.getAction(),
                    t.getRecordHash(), t.getPreviousHash(), e.getCreatedAt(), t.getAuthorityId(), t.getFabricTxId(),
                    t.getStatus(), t.getIntegrityStatus());
        }).toList();
    }

    @PostMapping("/verify")
    public ChainVerificationService.Report verify() { return verifier.verifyAll(); }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of("mode", ledger.mode(),
                "pending", txs.countByStatus("PENDING"),
                "committed", txs.countByStatus("COMMITTED"),
                "failed", txs.countByStatus("FAILED"));
    }

    @GetMapping("/authority-public-key")
    public Map<String, String> publicKey() {
        return Map.of("algorithm", "ECDSA P-256 (SHA256withECDSA)", "publicKeyBase64", crypto.publicKeyBase64());
    }
}
