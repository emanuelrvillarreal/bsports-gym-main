package com.blue.open.Code.Service;

import com.blue.open.Core.Exceptions.CustomException;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Service
public class QrCodeService {

    private static final int QR_SIZE = 256;

    public String generateBase64Png(String content) {
        if (content == null || content.isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Contenido QR requerido",
                    "QrCodeService | content vacío");
        }
        try {
            BitMatrix matrix = new QRCodeWriter().encode(content.trim(), BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al generar el QR",
                    "QrCodeService | " + e.getMessage());
        }
    }
}
