package com.votexa;

import com.votexa.common.CryptoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Hosted mode: keys supplied by environment variables must behave like the file-based keys. */
class CryptoServiceEnvKeyTest {
    @Test
    void keysFromEnvironmentWorkLikeFileKeys(@TempDir Path dir) throws Exception {
        CryptoService fileBased = new CryptoService(dir.toString());
        String pub = fileBased.publicKeyBase64();
        String priv = Files.readString(dir.resolve("authority-private.b64")).trim();

        CryptoService envBased = new CryptoService(dir.resolve("unused").toString(), priv, pub);
        byte[] data = CryptoService.utf8("votexa");

        assertEquals(pub, envBased.publicKeyBase64());
        assertTrue(fileBased.verify(data, envBased.sign(data)));
        assertFalse(Files.exists(dir.resolve("unused")), "env mode must not write key files");
    }
}
