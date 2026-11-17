package com.blue.open.Core.Service;

import com.blue.open.Core.Entity.DTO.DeleteArchivoRequest;
import com.blue.open.Core.Exceptions.CustomException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class SM3Service {
    @Value("${sm3.url:#{null}}")
    private String crypter;

    @Value("${sm3.user:#{null}}")
    private String user;

    @Value("${sm3.pass:#{null}}")
    private String pass;

    @Value("${sm3.proyecto:#{null}}")
    private String proyecto;

    private void validateConfiguration() {
        if (crypter == null || user == null || pass == null || proyecto == null) {
            throw new CustomException("Faltan configuraciones necesarias para el servicio SM3", "No existe sm3.url, sm3.user, sm3.pass o sm3.proyecto en el application.properties");
        }
    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String auth = user + ":" + pass;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + new String(encodedAuth);
        headers.set("Authorization", authHeader);
        return headers;
    }

    // Sube un archivo al servidor, espera un MultipartFile y reciba la url pública como respuesta
    public String upload(MultipartFile file) {
        this.validateConfiguration();

        try {
            RestTemplate restTemplate = new RestTemplate();

            // headers (sin Content-Type)
            HttpHeaders headers = getHeaders();

            // body multipart
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            body.add("file", fileResource);
            body.add("proyecto", proyecto);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    crypter + "/archivo",
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            return response.getBody();
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Error subiendo archivo al servidor de imagenes", "SM3 upload: " + e.getMessage());
        }
    }

    // Borra un achivo del servidor. Espera la url pública como paraemtro y responda un true cuando se borró satisfactoriamente
    public Boolean delete(DeleteArchivoRequest request) {
        this.validateConfiguration();

        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = getHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<DeleteArchivoRequest> requestEntity = new HttpEntity<>(request, headers);

            ResponseEntity<Boolean> response = restTemplate.exchange(
                    crypter + "/archivo",
                    HttpMethod.DELETE,
                    requestEntity,
                    Boolean.class
            );

            return response.getBody();
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Error eliminando archivo en el servidor de imágenes", "SM3 delete: " + e.getMessage());
        }
    }
}
