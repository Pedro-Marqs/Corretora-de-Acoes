package com.projeto.gestao.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Preserves BCrypt as the password-storage algorithm while pre-hashing input so
 * passwords longer than BCrypt's 72-byte input limit remain supported.
 */
final class PreHashingBCryptPasswordEncoder extends BCryptPasswordEncoder {
    @Override
    public String encode(CharSequence rawPassword) {
        return super.encode(preHash(rawPassword));
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return super.matches(preHash(rawPassword), encodedPassword);
    }

    private String preHash(CharSequence rawPassword) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawPassword.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 não está disponível.", exception);
        }
    }
}
