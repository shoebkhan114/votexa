package com.votexa.blockchain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.votexa.common.Hashing;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Development ledger: an append-only, hash-chained JSON-lines file kept OUTSIDE the database.
 * It lets the whole system run without Fabric. It is NOT a distributed blockchain.
 */
@Component
@ConditionalOnProperty(name = "votexa.ledger.mode", havingValue = "local", matchIfMissing = true)
public class LocalLedgerClient implements LedgerClient {
    private final Path file;
    private final ObjectMapper om = new ObjectMapper();

    public LocalLedgerClient(@Value("${votexa.ledger.local-file}") String path) throws IOException {
        this.file = Path.of(path);
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        if (!Files.exists(file)) Files.createFile(file);
    }

    private List<String> lines() throws IOException {
        return Files.readAllLines(file).stream().filter(l -> !l.isBlank()).toList();
    }

    @Override
    public synchronized String submit(LedgerRecord r) throws Exception {
        List<String> lines = lines();
        for (String l : lines) {
            JsonNode n = om.readTree(l);
            if (n.path("eventId").asText().equals(r.eventId())) {
                if (n.path("recordHash").asText().equals(r.recordHash())) return n.path("txId").asText();
                throw new IllegalStateException("Event already on ledger with a different hash: " + r.eventId());
            }
        }
        String prevLineHash = lines.isEmpty() ? Hashing.ZERO : Hashing.sha256Hex(lines.get(lines.size() - 1));
        String txId = "LOCAL-" + Hashing.sha256Hex(r.eventId() + r.recordHash()).substring(0, 20);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("txId", txId);
        m.put("eventId", r.eventId());
        m.put("eventType", r.eventType());
        m.put("recordHash", r.recordHash());
        m.put("previousHash", r.previousHash());
        m.put("timestamp", r.timestamp());
        m.put("authorityId", r.authorityId());
        m.put("signature", r.signature());
        m.put("prevLineHash", prevLineHash);
        Files.writeString(file, om.writeValueAsString(m) + "\n", StandardOpenOption.APPEND);
        return txId;
    }

    @Override
    public synchronized Optional<String> fetchRecordHash(String eventId) throws Exception {
        for (String l : lines()) {
            JsonNode n = om.readTree(l);
            if (n.path("eventId").asText().equals(eventId)) return Optional.of(n.path("recordHash").asText());
        }
        return Optional.empty();
    }

    @Override
    public synchronized long recordCount() throws Exception { return lines().size(); }

    @Override
    public String mode() { return "local"; }
}
