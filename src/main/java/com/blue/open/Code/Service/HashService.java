package com.blue.open.Code.Service;

import com.blue.open.Core.Exceptions.CustomException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Hash SHA-256 de textos, devuelto en hexadecimal (64 caracteres).
 */
@Service
public class HashService {

    private static final String ALGORITMO = "SHA-256";

    public String hashear(String texto) {
        if (texto == null) {
            throw new CustomException("No se pudo procesar el código", "HashService | hashear | texto nulo");
        }

        MessageDigest digest = crearDigest();
        byte[] hash = digest.digest(texto.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    private MessageDigest crearDigest() {
        try {
            return MessageDigest.getInstance(ALGORITMO);
        } catch (NoSuchAlgorithmException ex) {
            throw new CustomException("No se pudo procesar el código",
                    "HashService | crearDigest | algoritmo no disponible: " + ALGORITMO);
        }
    }
}
