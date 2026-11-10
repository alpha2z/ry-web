package org.dromara.game.service;

import org.dromara.game.adapter.HmacSigner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class SecurityUtilitiesTest {
    @Test void credentialsRoundTripWithAuthenticatedEncryption() {
        CredentialCrypto crypto = new CredentialCrypto("0123456789abcdef0123456789abcdef");
        String cipher = crypto.encrypt("server-secret");
        assertNotEquals("server-secret", cipher);
        assertEquals("server-secret", crypto.decrypt(cipher));
        assertNotEquals(cipher, crypto.encrypt("server-secret"));
    }
    @Test void hmacSignatureIsDeterministicAndCoversBody() {
        String a = HmacSigner.sign("secret", "100", "nonce", "POST", "/path", "{}");
        assertEquals(a, HmacSigner.sign("secret", "100", "nonce", "POST", "/path", "{}"));
        assertNotEquals(a, HmacSigner.sign("secret", "100", "nonce", "POST", "/path", "{\"x\":1}"));
    }
}
