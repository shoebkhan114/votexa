package com.votexa.offline.crypto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.AEADBadTagException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PublicKey;
import java.time.Instant;

/** Verify signature -> decrypt -> validate package, exactly in the order described in the VOTEXA design. */
public final class PackageVerifier {
    private static final ObjectMapper OM = new ObjectMapper();
    private PackageVerifier() {}

    public record OpenedPackage(JsonNode plain, String packageId, String electionCode, String stationCode,
                                String stationName, Instant expiresAt, String snapshotHash, String sha256) {}

    public static OpenedPackage open(Path packageFile, String authorityPublicKeyB64, String passphrase) throws Exception {
        JsonNode env = OM.readTree(Files.readAllBytes(packageFile));
        if (!"VOTEXA-PKG-1".equals(env.path("format").asText()))
            throw new SecurityException("Unsupported or corrupt package format");

        byte[] plain;
        try {
            plain = CryptoCore.decrypt(env.path("salt").asText(), env.path("iv").asText(), env.path("ciphertext").asText(),
                    env.path("kdfIterations").asInt(), passphrase);
        } catch (AEADBadTagException e) {
            throw new SecurityException("Decryption failed: wrong passphrase or the package was modified");
        }

        String sha = CryptoCore.sha256Hex(plain);
        if (!sha.equals(env.path("plaintextSha256").asText()))
            throw new SecurityException("Package hash mismatch: package content was altered");

        PublicKey key = CryptoCore.publicKey(authorityPublicKeyB64);
        if (!CryptoCore.verify(plain, env.path("signature").asText(), key))
            throw new SecurityException("INVALID SIGNATURE: package was not issued by the election authority");

        JsonNode p = OM.readTree(new String(plain, StandardCharsets.UTF_8));
        if (!p.path("packageId").asText().equals(env.path("packageId").asText())
                || !p.path("stationCode").asText().equals(env.path("stationCode").asText()))
            throw new SecurityException("Package header does not match its signed content");

        Instant expires = Instant.parse(p.path("expiresAt").asText());
        if (Instant.now().isAfter(expires)) throw new SecurityException("Package expired at " + expires);

        return new OpenedPackage(p, p.path("packageId").asText(), p.path("electionCode").asText(),
                p.path("stationCode").asText(), p.path("stationName").asText(), expires, p.path("snapshotHash").asText(), sha);
    }
}
