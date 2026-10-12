package com.votexa.offline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.votexa.offline.crypto.CryptoCore;
import com.votexa.offline.crypto.PackageVerifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Signature;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Builds a package exactly like the backend's PackageService/CryptoService and opens it with the offline verifier. */
class PackageVerifierTest {
    private static final ObjectMapper OM = new ObjectMapper();

    private record Built(Path file, String publicKeyB64) {}

    private Built build(Path dir, String pass, Instant expires, KeyPair signer) throws Exception {
        Map<String, Object> plain = new LinkedHashMap<>();
        plain.put("packageId", "pkg-1");
        plain.put("electionCode", "ELC-TEST");
        plain.put("rollVersion", 1);
        plain.put("snapshotHash", "abc");
        plain.put("stationCode", "PS-102");
        plain.put("stationName", "Test School");
        plain.put("issuedAt", Instant.now().toString());
        plain.put("expiresAt", expires.toString());
        plain.put("voters", List.of(Map.of("voterCode", "VTX0000001", "fullName", "Aarav Sharma", "votingStatus", "NOT_VOTED")));
        byte[] bytes = OM.writeValueAsBytes(plain);

        Signature s = Signature.getInstance("SHA256withECDSA");
        s.initSign(signer.getPrivate());
        s.update(bytes);
        String sig = Base64.getEncoder().encodeToString(s.sign());

        SecureRandom r = new SecureRandom();
        byte[] salt = new byte[16], iv = new byte[12];
        r.nextBytes(salt); r.nextBytes(iv);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.ENCRYPT_MODE, CryptoCore.deriveKey(pass, salt, 210_000), new GCMParameterSpec(128, iv));
        byte[] ct = c.doFinal(bytes);

        Map<String, Object> env = new LinkedHashMap<>();
        env.put("format", "VOTEXA-PKG-1");
        env.put("packageId", "pkg-1");
        env.put("stationCode", "PS-102");
        env.put("plaintextSha256", CryptoCore.sha256Hex(bytes));
        env.put("signature", sig);
        env.put("kdfIterations", 210_000);
        env.put("salt", Base64.getEncoder().encodeToString(salt));
        env.put("iv", Base64.getEncoder().encodeToString(iv));
        env.put("ciphertext", Base64.getEncoder().encodeToString(ct));
        Path f = dir.resolve("t.vxpkg");
        Files.write(f, OM.writeValueAsBytes(env));
        return new Built(f, Base64.getEncoder().encodeToString(signer.getPublic().getEncoded()));
    }

    private KeyPair keys() throws Exception {
        KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
        g.initialize(256);
        return g.generateKeyPair();
    }

    @Test
    void validPackageOpens(@TempDir Path dir) throws Exception {
        Built b = build(dir, "passphrase-1", Instant.now().plus(1, ChronoUnit.DAYS), keys());
        var p = PackageVerifier.open(b.file(), b.publicKeyB64(), "passphrase-1");
        assertEquals("PS-102", p.stationCode());
        assertEquals(1, p.plain().path("voters").size());
    }

    @Test
    void wrongPassphraseIsRejected(@TempDir Path dir) throws Exception {
        Built b = build(dir, "passphrase-1", Instant.now().plus(1, ChronoUnit.DAYS), keys());
        assertThrows(SecurityException.class, () -> PackageVerifier.open(b.file(), b.publicKeyB64(), "nope-nope-1"));
    }

    @Test
    void packageSignedByAnotherAuthorityIsRejected(@TempDir Path dir) throws Exception {
        Built b = build(dir, "passphrase-1", Instant.now().plus(1, ChronoUnit.DAYS), keys());
        String otherPub = Base64.getEncoder().encodeToString(keys().getPublic().getEncoded());
        var ex = assertThrows(SecurityException.class, () -> PackageVerifier.open(b.file(), otherPub, "passphrase-1"));
        assertTrue(ex.getMessage().contains("INVALID SIGNATURE"));
    }

    @Test
    void expiredPackageIsRejected(@TempDir Path dir) throws Exception {
        Built b = build(dir, "passphrase-1", Instant.now().minus(1, ChronoUnit.HOURS), keys());
        assertThrows(SecurityException.class, () -> PackageVerifier.open(b.file(), b.publicKeyB64(), "passphrase-1"));
    }

    @Test
    void tamperedCiphertextIsRejected(@TempDir Path dir) throws Exception {
        Built b = build(dir, "passphrase-1", Instant.now().plus(1, ChronoUnit.DAYS), keys());
        @SuppressWarnings("unchecked")
        Map<String, Object> env = OM.readValue(Files.readAllBytes(b.file()), Map.class);
        byte[] ct = Base64.getDecoder().decode((String) env.get("ciphertext"));
        ct[5] ^= 0x01;
        env.put("ciphertext", Base64.getEncoder().encodeToString(ct));
        Files.write(b.file(), OM.writeValueAsBytes(env));
        assertThrows(SecurityException.class, () -> PackageVerifier.open(b.file(), b.publicKeyB64(), "passphrase-1"));
    }
}
