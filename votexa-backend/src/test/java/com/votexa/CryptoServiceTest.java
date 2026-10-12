package com.votexa;

import com.votexa.common.CryptoService;
import com.votexa.common.Hashing;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CryptoServiceTest {
    @Test
    void signVerifyAndTamperDetection(@TempDir Path dir) throws Exception {
        CryptoService c = new CryptoService(dir.toString());
        byte[] data = "election package".getBytes();
        String sig = c.sign(data);
        assertTrue(c.verify(data, sig));
        assertFalse(c.verify("election packagE".getBytes(), sig));
    }

    @Test
    void encryptDecryptRoundTripAndWrongPassphrase(@TempDir Path dir) throws Exception {
        CryptoService c = new CryptoService(dir.toString());
        byte[] plain = "{\"voters\":[]}".getBytes();
        CryptoService.Encrypted e = c.encrypt(plain, "correct-passphrase");
        assertArrayEquals(plain, c.decrypt(e, "correct-passphrase"));
        assertThrows(IllegalStateException.class, () -> c.decrypt(e, "wrong-passphrase"));
    }

    @Test
    void keysArePersistedAndReloaded(@TempDir Path dir) throws Exception {
        String pk1 = new CryptoService(dir.toString()).publicKeyBase64();
        String pk2 = new CryptoService(dir.toString()).publicKeyBase64();
        assertEquals(pk1, pk2);
    }

    @Test
    void sha256KnownVector() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", Hashing.sha256Hex("abc"));
    }
}
