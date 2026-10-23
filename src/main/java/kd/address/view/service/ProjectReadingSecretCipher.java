package kd.address.view.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class ProjectReadingSecretCipher {
    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${portal.ai.encryption-key:}")
    private String encryptionKey;

    public String encrypt(String plaintext) {
        try {
            byte[] nonce = new byte[12];
            RANDOM.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey(), new GCMParameterSpec(128, nonce));
            byte[] encrypted = cipher.doFinal((plaintext == null ? "" : plaintext).getBytes(StandardCharsets.UTF_8));
            byte[] combined = Arrays.copyOf(nonce, nonce.length + encrypted.length);
            System.arraycopy(encrypted, 0, combined, nonce.length, encrypted.length);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(combined);
        } catch (Exception exception) {
            throw new IllegalStateException("无法加密解读引擎密钥，请检查 PORTAL_AI_ENCRYPTION_KEY", exception);
        }
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isBlank()) return "";
        try {
            byte[] combined = Base64.getUrlDecoder().decode(ciphertext);
            if (combined.length <= 12) throw new IllegalArgumentException("加密内容格式无效");
            byte[] nonce = Arrays.copyOfRange(combined, 0, 12);
            byte[] encrypted = Arrays.copyOfRange(combined, 12, combined.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), new GCMParameterSpec(128, nonce));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("无法解密解读引擎密钥，请检查 PORTAL_AI_ENCRYPTION_KEY", exception);
        }
    }

    private SecretKeySpec secretKey() throws Exception {
        if (encryptionKey == null || encryptionKey.isBlank())
            throw new IllegalStateException("尚未配置 PORTAL_AI_ENCRYPTION_KEY");
        String normalizedKey = encryptionKey.trim();
        if (normalizedKey.length() < 32)
            throw new IllegalStateException("PORTAL_AI_ENCRYPTION_KEY 至少需要 32 个字符");
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(normalizedKey.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(digest, "AES");
    }
}
