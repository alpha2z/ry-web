package org.dromara.game.service;

import org.dromara.common.core.exception.ServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class CredentialCrypto {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final SecretKeySpec key;

    public CredentialCrypto(@Value("${game.security.master-key}") String masterKey) {
        try {
            this.key = new SecretKeySpec(MessageDigest.getInstance("SHA-256")
                .digest(masterKey.getBytes(StandardCharsets.UTF_8)), "AES");
        } catch (Exception e) {
            throw new IllegalStateException("无法初始化区服凭据加密器", e);
        }
    }

    public String encrypt(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            byte[] iv = new byte[12];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new ServiceException("区服凭据加密失败");
        }
    }

    public String decrypt(String value) {
        if (value == null || value.isBlank()) return "";
        try {
            byte[] combined = Base64.getDecoder().decode(value);
            byte[] iv = java.util.Arrays.copyOfRange(combined, 0, 12);
            byte[] encrypted = java.util.Arrays.copyOfRange(combined, 12, combined.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new ServiceException("区服凭据解密失败，请检查 GAME_MASTER_KEY");
        }
    }
}
