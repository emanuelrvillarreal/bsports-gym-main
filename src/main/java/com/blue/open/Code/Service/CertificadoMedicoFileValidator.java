package com.blue.open.Code.Service;

import com.blue.open.Core.Exceptions.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Service
public class CertificadoMedicoFileValidator {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/png", "image/jpeg", "application/pdf");
    private static final long MAX_SIZE_BYTES = 2L * 1024 * 1024;

    public void validar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Archivo requerido",
                    "Debe enviar un archivo para el certificado médico.");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "El archivo no puede superar los 2MB",
                    "CertificadoMedicoFileValidator | size: " + file.getSize());
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "Tipo de archivo no permitido (solo PNG, JPEG, JPG o PDF)",
                    "CertificadoMedicoFileValidator | contentType: " + contentType);
        }
    }
}
