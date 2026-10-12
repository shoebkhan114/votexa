package com.votexa.offline.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;

/** JDK-only crypto primitives; parameters MUST match the backend CryptoService. */
public final class CryptoCore {
    private CryptoCore() {}

    public static SecretKey deriveKey(String passphrase, byte[] salt, int iterations) throws GeneralSecurityException {
        SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] k = f.generateSecret(new PBEKeySpec(passphrase.toCharArray(), salt, iterations, 256)).getEncoded();
        return new SecretKeySpec(k, "AES");
    }

    public static byte[] decrypt(String saltB64, String ivB64, String ctB64, int iterations, String passphrase) throws GeneralSecurityException {
        Base64.Decoder d = Base64.getDecoder();
        SecretKey key = deriveKey(passphrase, d.decode(saltB64), iterations);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, d.decode(ivB64)));
        return c.doFinal(d.decode(ctB64));
    }

    public static PublicKey publicKey(String b64) throws GeneralSecurityException {
        return KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(b64.trim())));
    }

    public static boolean verify(byte[] data, String sigB64, PublicKey key) {
        try {
            Signature s = Signature.getInstance("SHA256withECDSA");
            s.initVerify(key);
            s.update(data);
            return s.verify(Base64.getDecoder().decode(sigB64));
        } catch (Exception e) {
            return false;
        }
    }

    public static String sha256Hex(byte[] data) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data)); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
}
