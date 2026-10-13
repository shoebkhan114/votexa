package com.votexa.blockchain;

import com.votexa.common.Hashing;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Hosted-demo ledger: an append-only, hash-chained table (ledger_records) kept next to the data.
 * It survives restarts on hosts with a temporary file system. It is NOT a distributed blockchain
 * and is less independent than a separate ledger or Hyperledger Fabric.
 */
@Component
@ConditionalOnProperty(name = "votexa.ledger.mode", havingValue = "db")
public class DbLedgerClient implements LedgerClient {
    private final JdbcTemplate jdbc;

    public DbLedgerClient(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public synchronized String submit(LedgerRecord r) {
        List<String[]> existing = jdbc.query(
                "SELECT record_hash, tx_id FROM ledger_records WHERE event_id = ?",
                (rs, i) -> new String[]{rs.getString(1), rs.getString(2)}, r.eventId());
        if (!existing.isEmpty()) {
            String[] row = existing.get(0);
            if (row[0].equals(r.recordHash())) return row[1];
            throw new IllegalStateException("Event already on ledger with a different hash: " + r.eventId());
        }
        List<String> last = jdbc.query("SELECT line_hash FROM ledger_records ORDER BY seq DESC LIMIT 1",
                (rs, i) -> rs.getString(1));
        String prevLineHash = last.isEmpty() ? Hashing.ZERO : last.get(0);
        String txId = "DB-" + Hashing.sha256Hex(r.eventId() + r.recordHash()).substring(0, 20);
        String lineHash = Hashing.sha256Hex(String.join("|", txId, r.eventId(), r.eventType(), r.recordHash(),
                String.valueOf(r.previousHash()), String.valueOf(r.timestamp()), String.valueOf(r.authorityId()),
                String.valueOf(r.signature()), prevLineHash));
        jdbc.update("INSERT INTO ledger_records (event_id, tx_id, event_type, record_hash, previous_hash, "
                        + "event_timestamp, authority_id, signature, prev_line_hash, line_hash) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                r.eventId(), txId, r.eventType(), r.recordHash(), r.previousHash(),
                r.timestamp(), r.authorityId(), r.signature(), prevLineHash, lineHash);
        return txId;
    }

    @Override
    public synchronized Optional<String> fetchRecordHash(String eventId) {
        List<String> rows = jdbc.query("SELECT record_hash FROM ledger_records WHERE event_id = ?",
                (rs, i) -> rs.getString(1), eventId);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    @Override
    public synchronized long recordCount() {
        Long c = jdbc.queryForObject("SELECT COUNT(*) FROM ledger_records", Long.class);
        return c == null ? 0 : c;
    }

    @Override
    public String mode() {
        return "db";
    }
}
