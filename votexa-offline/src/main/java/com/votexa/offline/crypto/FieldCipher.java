package com.votexa.offline.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/** AES-256-GCM field-level encryption for sensitive columns in the local SQLite database. */
public final class FieldCipher {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final SecretKey key;

    public FieldCipher(String passphrase, byte[] deviceSalt) throws GeneralSecurityException {
        this.key = CryptoCore.deriveKey(passphrase, deviceSalt, 210_000);
    }

    public String encrypt(String plain) {
        try {
            byte[] iv = new byte[12];
            RANDOM.nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] ct = c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + ct.length).put(iv).put(ct).array());
        } catch (GeneralSecurityException e) { throw new IllegalStateException(e); }
    }

    public String decrypt(String b64) throws GeneralSecurityException {
        byte[] all = Base64.getDecoder().decode(b64);
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, all, 0, 12));
        return new String(c.doFinal(all, 12, all.length - 12), StandardCharsets.UTF_8);
    }
}
