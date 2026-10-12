package com.votexa.offline.database;

import com.fasterxml.jackson.databind.JsonNode;
import com.votexa.offline.crypto.FieldCipher;
import com.votexa.offline.crypto.PackageVerifier.OpenedPackage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/**
 * Local SQLite store for ONE polling station. Tables: offline_voters, polling_config, offline_events,
 * sync_queue, device_info. Voter names are encrypted field-by-field (AES-256-GCM).
 * Note: full-file encryption (e.g. SQLCipher) is a possible hardening step.
 */
public final class LocalDb implements AutoCloseable {
    public record VoterRow(String code, String name, String votingStatus, String authorizedAt) {}
    public record SyncRow(String voterCode, String authorizedAt) {}

    private final Connection conn;
    private final FieldCipher cipher;

    private LocalDb(Connection conn, FieldCipher cipher) { this.conn = conn; this.cipher = cipher; }

    public static Path dbPath(Path dir, String stationCode) { return dir.resolve(stationCode + ".db"); }

    private static Connection connect(Path file) throws SQLException {
        Connection c = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
        try (Statement s = c.createStatement()) { s.execute("PRAGMA journal_mode=WAL"); s.execute("PRAGMA foreign_keys=ON"); }
        return c;
    }

    private static void schema(Connection c) throws SQLException {
        try (Statement s = c.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS device_info (key TEXT PRIMARY KEY, value TEXT NOT NULL)");
            s.execute("CREATE TABLE IF NOT EXISTS polling_config (key TEXT PRIMARY KEY, value TEXT NOT NULL)");
            s.execute("CREATE TABLE IF NOT EXISTS offline_voters (voter_code TEXT PRIMARY KEY, name_enc TEXT NOT NULL, " +
                    "voting_status TEXT NOT NULL CHECK (voting_status IN ('NOT_VOTED','VOTED')), authorized_at TEXT)");
            s.execute("CREATE TABLE IF NOT EXISTS offline_events (id INTEGER PRIMARY KEY AUTOINCREMENT, voter_code TEXT, " +
                    "event_type TEXT NOT NULL, result TEXT NOT NULL, created_at TEXT NOT NULL)");
            s.execute("CREATE TABLE IF NOT EXISTS sync_queue (id INTEGER PRIMARY KEY AUTOINCREMENT, voter_code TEXT NOT NULL UNIQUE, " +
                    "authorized_at TEXT NOT NULL, synced INTEGER NOT NULL DEFAULT 0)");
        }
    }

    /** Creates a fresh station database from a verified package. */
    public static LocalDb create(Path dir, OpenedPackage pkg, String passphrase) throws Exception {
        Files.createDirectories(dir);
        Path file = dbPath(dir, pkg.stationCode());
        if (Files.exists(file)) throw new IllegalStateException("A session for " + pkg.stationCode() + " already exists on this device. Use 'Resume session'.");
        Connection c = connect(file);
        schema(c);
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        FieldCipher fc = new FieldCipher(passphrase, salt);
        c.setAutoCommit(false);
        try {
            put(c, "device_info", "device_id", "DEV-" + UUID.randomUUID().toString().substring(0, 8));
            put(c, "device_info", "salt", Base64.getEncoder().encodeToString(salt));
            put(c, "device_info", "canary", fc.encrypt("VOTEXA"));
            put(c, "polling_config", "package_id", pkg.packageId());
            put(c, "polling_config", "election_code", pkg.electionCode());
            put(c, "polling_config", "station_code", pkg.stationCode());
            put(c, "polling_config", "station_name", pkg.stationName());
            put(c, "polling_config", "expires_at", pkg.expiresAt().toString());
            put(c, "polling_config", "snapshot_hash", pkg.snapshotHash());
            put(c, "polling_config", "loaded_at", Instant.now().toString());
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO offline_voters(voter_code, name_enc, voting_status) VALUES (?,?,?)")) {
                for (JsonNode v : pkg.plain().path("voters")) {
                    ps.setString(1, v.path("voterCode").asText());
                    ps.setString(2, fc.encrypt(v.path("fullName").asText()));
                    ps.setString(3, "VOTED".equals(v.path("votingStatus").asText()) ? "VOTED" : "NOT_VOTED");
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            c.commit();
        } catch (Exception e) {
            c.rollback();
            c.close();
            Files.deleteIfExists(file);
            throw e;
        }
        c.setAutoCommit(true);
        return new LocalDb(c, fc);
    }

    /** Re-opens an existing station database; the passphrase is verified against an encrypted canary. */
    public static LocalDb open(Path dir, String stationCode, String passphrase) throws Exception {
        Path file = dbPath(dir, stationCode);
        if (!Files.exists(file)) throw new IllegalStateException("No local session for " + stationCode);
        Connection c = connect(file);
        schema(c);
        byte[] salt = Base64.getDecoder().decode(get(c, "device_info", "salt"));
        FieldCipher fc = new FieldCipher(passphrase, salt);
        try {
            if (!"VOTEXA".equals(fc.decrypt(get(c, "device_info", "canary")))) throw new GeneralSecurityException();
        } catch (GeneralSecurityException e) {
            c.close();
            throw new SecurityException("Wrong passphrase for this local database");
        }
        return new LocalDb(c, fc);
    }

    public static List<String> existingStations(Path dir) {
        try {
            if (!Files.isDirectory(dir)) return List.of();
            try (var s = Files.list(dir)) {
                return s.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".db"))
                        .map(n -> n.substring(0, n.length() - 3)).sorted().toList();
            }
        } catch (Exception e) { return List.of(); }
    }

    private static void put(Connection c, String table, String k, String v) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT OR REPLACE INTO " + table + "(key, value) VALUES (?,?)")) {
            ps.setString(1, k); ps.setString(2, v); ps.executeUpdate();
        }
    }

    private static String get(Connection c, String table, String k) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT value FROM " + table + " WHERE key = ?")) {
            ps.setString(1, k);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getString(1) : null; }
        }
    }

    public String config(String key) throws SQLException { return get(conn, "polling_config", key); }
    public String device(String key) throws SQLException { return get(conn, "device_info", key); }

    public Optional<VoterRow> find(String code) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("SELECT name_enc, voting_status, authorized_at FROM offline_voters WHERE voter_code = ?")) {
            ps.setString(1, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new VoterRow(code, cipher.decrypt(rs.getString(1)), rs.getString(2), rs.getString(3)));
            }
        }
    }

    /** Atomic NOT_VOTED -> VOTED. Returns false if the voter was already authorized. */
    public boolean authorize(String code) throws SQLException {
        String now = Instant.now().toString();
        conn.setAutoCommit(false);
        try {
            int n;
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE offline_voters SET voting_status='VOTED', authorized_at=? WHERE voter_code=? AND voting_status='NOT_VOTED'")) {
                ps.setString(1, now); ps.setString(2, code); n = ps.executeUpdate();
            }
            if (n == 1) {
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO sync_queue(voter_code, authorized_at) VALUES (?,?)")) {
                    ps.setString(1, code); ps.setString(2, now); ps.executeUpdate();
                }
            }
            conn.commit();
            return n == 1;
        } catch (SQLException e) { conn.rollback(); throw e; }
        finally { conn.setAutoCommit(true); }
    }

    public void logEvent(String code, String type, String result) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO offline_events(voter_code, event_type, result, created_at) VALUES (?,?,?,?)")) {
            ps.setString(1, code); ps.setString(2, type); ps.setString(3, result); ps.setString(4, Instant.now().toString()); ps.executeUpdate();
        }
    }

    public int count(String where) throws SQLException {
        try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery("SELECT count(*) FROM offline_voters" + (where == null ? "" : " WHERE " + where))) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public List<SyncRow> pendingSync() throws SQLException {
        List<SyncRow> out = new ArrayList<>();
        try (Statement s = conn.createStatement(); ResultSet rs = s.executeQuery("SELECT voter_code, authorized_at FROM sync_queue WHERE synced = 0 ORDER BY id")) {
            while (rs.next()) out.add(new SyncRow(rs.getString(1), rs.getString(2)));
        }
        return out;
    }

    public void markSynced() throws SQLException {
        try (Statement s = conn.createStatement()) { s.executeUpdate("UPDATE sync_queue SET synced = 1 WHERE synced = 0"); }
    }

    @Override
    public void close() throws SQLException { conn.close(); }
}
