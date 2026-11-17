package com.blue.open.Code.Service;

import com.blue.open.Core.Exceptions.CustomException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class MercadoPagoCredentialsCryptoService {

    public static final String ACCESS_TOKEN_FIELD = "access_token";
    public static final String WEBHOOK_SECRET_FIELD = "webhook_secret";

    private static final String ENCRYPTED_PREFIX = "enc:";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKey encryptionKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public MercadoPagoCredentialsCryptoService(
            @Value("${mercado-pago.credentials.encryption-key}") String encryptionKeyBase64) {
        this.encryptionKey = parseEncryptionKey(encryptionKeyBase64);
    }

    public String encrypt(String value, String companyUuid, String field) {
        if (value == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            cipher.updateAAD(buildAad(companyUuid, field));
            byte[] ciphertext = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(ciphertext, 0, payload, iv.length, ciphertext.length);
            return ENCRYPTED_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
        } catch (GeneralSecurityException ex) {
            throw credentialFailure(ex);
        }
    }

    public String decrypt(String encryptedValue, String companyUuid, String field) {
        if (encryptedValue == null) {
            return null;
        }
        try {
            if (!isEncrypted(encryptedValue)) {
                throw new IllegalArgumentException("Encrypted credential format is invalid");
            }
            byte[] payload = Base64.getUrlDecoder().decode(encryptedValue.substring(ENCRYPTED_PREFIX.length()));
            if (payload.length <= IV_LENGTH_BYTES) {
                throw new IllegalArgumentException("Encrypted credential IV is invalid");
            }
            byte[] iv = new byte[IV_LENGTH_BYTES];
            byte[] ciphertext = new byte[payload.length - IV_LENGTH_BYTES];
            System.arraycopy(payload, 0, iv, 0, iv.length);
            System.arraycopy(payload, iv.length, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            cipher.updateAAD(buildAad(companyUuid, field));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw credentialFailure(ex);
        }
    }

    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(ENCRYPTED_PREFIX);
    }

    private SecretKey parseEncryptionKey(String encryptionKeyBase64) {
        try {
            if (encryptionKeyBase64 == null || encryptionKeyBase64.isBlank()) {
                throw new IllegalStateException("Falta mercado-pago.credentials.encryption-key");
            }
            byte[] key = Base64.getDecoder().decode(encryptionKeyBase64);
            if (key.length != 32) {
                throw new IllegalStateException("La clave de cifrado de Mercado Pago debe tener 32 bytes");
            }
            return new SecretKeySpec(key, "AES");
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("La clave de cifrado de Mercado Pago debe estar codificada en Base64", ex);
        }
    }

    private byte[] buildAad(String companyUuid, String field) {
        if (companyUuid == null || companyUuid.isBlank() || field == null || field.isBlank()) {
            throw new IllegalArgumentException("Contexto de credencial inválido");
        }
        return (companyUuid + ":" + field).getBytes(StandardCharsets.UTF_8);
    }

    private CustomException credentialFailure(Exception cause) {
        return new CustomException(HttpStatus.INTERNAL_SERVER_ERROR,
                "No se pudo procesar una credencial de Mercado Pago",
                "MercadoPagoCredentialsCryptoService | error al procesar credencial: "
                        + cause.getClass().getSimpleName());
    }
}
