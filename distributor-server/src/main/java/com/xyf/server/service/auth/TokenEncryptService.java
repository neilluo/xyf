package com.xyf.server.service.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Token 加密服务 - AES-256-GCM
 * <p>
 * 密钥从环境变量 TOKEN_ENCRYPT_KEY 注入（32字节 Base64 编码）
 */
@Service
public class TokenEncryptService {

    private static final Logger log = LoggerFactory.getLogger(TokenEncryptService.class);
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    private final SecretKey secretKey;

    public TokenEncryptService(@Value("${token.encrypt.key:}") String keyBase64) {
        if (keyBase64 != null && !keyBase64.isBlank()) {
            byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
            this.secretKey = new SecretKeySpec(keyBytes, "AES");
        } else {
            // 开发模式：使用固定密钥（仅用于本地测试）
            log.warn("TOKEN_ENCRYPT_KEY not set, using dev key (NOT for production!)");
            byte[] devKey = "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8);
            this.secretKey = new SecretKeySpec(devKey, "AES");
        }
    }

    /**
     * 加密文本
     * @return Base64(iv + ciphertext + tag)
     */
    public String encrypt(String plainText) {
        if (plainText == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // iv + ciphertext concatenation
            byte[] result = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, result, 0, iv.length);
            System.arraycopy(cipherText, 0, result, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * 解密文本
     */
    public String decrypt(String encryptedBase64) {
        if (encryptedBase64 == null) return null;
        try {
            byte[] data = Base64.getDecoder().decode(encryptedBase64);

            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(data, 0, iv, 0, IV_LENGTH);

            byte[] cipherText = new byte[data.length - IV_LENGTH];
            System.arraycopy(data, IV_LENGTH, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
