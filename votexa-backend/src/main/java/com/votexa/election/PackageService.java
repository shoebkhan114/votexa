package com.votexa.election;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.votexa.audit.AuditService;
import com.votexa.auth.AuthUser;
import com.votexa.common.ApiException;
import com.votexa.common.CryptoService;
import com.votexa.common.Hashing;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds the offline election package:  snapshot JSON -> SHA-256 -> ECDSA signature -> AES-256-GCM encryption.
 * The offline validator verifies the signature with the authority PUBLIC key (delivered out-of-band).
 */
@Service
public class PackageService {
    private final ElectionService electionService;
    private final PollingStationRepository stations;
    private final JdbcTemplate jdbc;
    private final CryptoService crypto;
    private final AuditService audit;
    private final ObjectMapper om = new ObjectMapper();

    public PackageService(ElectionService electionService, PollingStationRepository stations, JdbcTemplate jdbc,
                          CryptoService crypto, AuditService audit) {
        this.electionService = electionService; this.stations = stations; this.jdbc = jdbc; this.crypto = crypto; this.audit = audit;
    }

    @Transactional
    public Map<String, Object> build(Long electionId, String stationCode, String passphrase, AuthUser actor) {
        Election e = electionService.get(electionId);
        if (!List.of("ROLL_FROZEN", "POLLING_OPEN", "POLLING_CLOSED").contains(e.getStatus()))
            throw ApiException.badRequest("The roll must be frozen before an offline package can be created");
        if (passphrase == null || passphrase.length() < 8) throw ApiException.badRequest("Passphrase must be at least 8 characters");
        PollingStation ps = stations.findByCode(stationCode.trim())
                .orElseThrow(() -> ApiException.notFound("Polling station not found: " + stationCode));

        List<Map<String, Object>> voters = jdbc.queryForList(
                "SELECT v.voter_code AS \"voterCode\", v.full_name AS \"fullName\", va.voting_status AS \"votingStatus\" " +
                "FROM voting_authorizations va JOIN voters v ON v.id = va.voter_id " +
                "JOIN polling_sessions s ON s.id = va.polling_session_id " +
                "WHERE va.election_id = ? AND s.polling_station_id = ? ORDER BY v.voter_code", e.getId(), ps.getId());
        if (voters.isEmpty()) throw ApiException.notFound("No voters for this station in the frozen roll");

        Instant issued = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant base = e.getPollingEnd() != null && e.getPollingEnd().isAfter(issued) ? e.getPollingEnd() : issued;
        String packageId = UUID.randomUUID().toString();

        Map<String, Object> plain = new LinkedHashMap<>();
        plain.put("packageId", packageId);
        plain.put("electionCode", e.getCode());
        plain.put("rollVersion", e.getRollVersion());
        plain.put("snapshotHash", e.getSnapshotHash());
        plain.put("stationCode", ps.getCode());
        plain.put("stationName", ps.getName());
        plain.put("issuedAt", issued.toString());
        plain.put("expiresAt", base.plus(24, ChronoUnit.HOURS).toString());
        plain.put("voters", voters);

        try {
            byte[] bytes = om.writeValueAsBytes(plain);
            String sha = Hashing.sha256Hex(bytes);
            String signature = crypto.sign(bytes);
            CryptoService.Encrypted enc = crypto.encrypt(bytes, passphrase);

            Map<String, Object> env = new LinkedHashMap<>();
            env.put("format", "VOTEXA-PKG-1");
            env.put("packageId", packageId);
            env.put("electionCode", e.getCode());
            env.put("stationCode", ps.getCode());
            env.put("plaintextSha256", sha);
            env.put("signatureAlgorithm", "SHA256withECDSA");
            env.put("signature", signature);
            env.put("authorityKeyFingerprint", Hashing.sha256Hex(crypto.publicKeyBase64()).substring(0, 16));
            env.put("cipher", "AES-256-GCM");
            env.put("kdf", "PBKDF2WithHmacSHA256");
            env.put("kdfIterations", enc.iterations());
            env.put("salt", enc.saltB64());
            env.put("iv", enc.ivB64());
            env.put("ciphertext", enc.cipherB64());

            audit.record("POLLING_PACKAGE_CREATED", "ELECTION_PACKAGE", packageId, actor, "PACKAGE_GENERATED", null, sha,
                    "Station " + ps.getCode() + ", " + voters.size() + " voters", "SUCCESS");
            return env;
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Could not build package", ex);
        }
    }
}
