package com.votexa.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Authority key pair (ECDSA P-256) + AES-256-GCM package encryption.
 * The private key never leaves ./keys; the public key is shared with offline devices.
 */
@Service
public class CryptoService {
    public static final int PBKDF2_ITERATIONS = 210_000;
    public record Encrypted(String saltB64, String ivB64, String cipherB64, int iterations) {}

    private final KeyPair keyPair;

    public CryptoService(@Value("${votexa.keys-dir}") String dir) throws Exception {
        Path d = Path.of(dir);
        Files.createDirectories(d);
        Path priv = d.resolve("authority-private.b64");
        Path pub = d.resolve("authority-public.b64");
        KeyFactory kf = KeyFactory.getInstance("EC");
        if (Files.exists(priv) && Files.exists(pub)) {
            PrivateKey sk = kf.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(Files.readString(priv).trim())));
            PublicKey pk = kf.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(Files.readString(pub).trim())));
            keyPair = new KeyPair(pk, sk);
        } else {
            KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
            g.initialize(256);
            keyPair = g.generateKeyPair();
            Files.writeString(priv, Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
            Files.writeString(pub, Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
            try { priv.toFile().setReadable(false, false); priv.toFile().setReadable(true, true); } catch (Exception ignored) { }
        }
    }

    public String publicKeyBase64() { return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()); }

    public String sign(byte[] data) {
        try {
            Signature s = Signature.getInstance("SHA256withECDSA");
            s.initSign(keyPair.getPrivate());
            s.update(data);
            return Base64.getEncoder().encodeToString(s.sign());
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    public boolean verify(byte[] data, String sigB64) {
        try {
            Signature s = Signature.getInstance("SHA256withECDSA");
            s.initVerify(keyPair.getPublic());
            s.update(data);
            return s.verify(Base64.getDecoder().decode(sigB64));
        } catch (Exception e) { return false; }
    }

    public Encrypted encrypt(byte[] plain, String passphrase) {
        try {
            SecureRandom r = new SecureRandom();
            byte[] salt = new byte[16]; r.nextBytes(salt);
            byte[] iv = new byte[12]; r.nextBytes(iv);
            SecretKey key = deriveKey(passphrase, salt, PBKDF2_ITERATIONS);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] ct = c.doFinal(plain);
            Base64.Encoder e = Base64.getEncoder();
            return new Encrypted(e.encodeToString(salt), e.encodeToString(iv), e.encodeToString(ct), PBKDF2_ITERATIONS);
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    public byte[] decrypt(Encrypted enc, String passphrase) {
        try {
            Base64.Decoder d = Base64.getDecoder();
            SecretKey key = deriveKey(passphrase, d.decode(enc.saltB64()), enc.iterations());
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, d.decode(enc.ivB64())));
            return c.doFinal(d.decode(enc.cipherB64()));
        } catch (Exception e) { throw new IllegalStateException("Decryption failed", e); }
    }

    static SecretKey deriveKey(String passphrase, byte[] salt, int iterations) throws Exception {
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] k = f.generateSecret(new PBEKeySpec(passphrase.toCharArray(), salt, iterations, 256)).getEncoded();
        return new SecretKeySpec(k, "AES");
    }

    public static byte[] utf8(String s) { return s.getBytes(StandardCharsets.UTF_8); }
}
