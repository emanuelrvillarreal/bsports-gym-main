package com.blue.open.Core.Service;

import com.blue.open.Core.Entity.DTO.ImagenCrypterDTO;
import com.blue.open.Core.Exceptions.CustomException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class CrypterService {
    @Value("${crypter.url:#{null}}")
    private String crypter;

    @Value("${crypter.user:#{null}}")
    private String user;

    @Value("${crypter.pass:#{null}}")
    private String pass;

    @Value("${crypter.tipo.documento:#{null}}")
    private String tipoDocumento;

    private void validateConfiguration() {
        if (crypter == null || user == null || pass == null || tipoDocumento == null) {
            throw new CustomException("Faltan configuraciones necesarias para el servicio S3", "No existe crypter.url, crypter.user, crypter.pass o crypter.tipo.documento en el application.properties");
        }
    }

    private HttpHeaders getHeaders() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            String auth = user + ":" + pass;
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
            String authHeader = "Basic " + new String(encodedAuth);
            headers.set("Authorization", authHeader);
            return headers;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Error al intentar obtener o gurdar el archivo", "CrypterService | getHeaders | error: " + e.getMessage());
        }
    }

    // Obtiene una lista de archivos, buscando por sus resourcekeys
    public List<String> getFiles(List<String> resourcekeys) {
        validateConfiguration();
        try {
            RestTemplate restTemplate = new RestTemplate();
            List<String> respuesta = new ArrayList<>();

            // Construir el cuerpo de la solicitud
            List<Map<String, String>> requestBodyList = new ArrayList<>();
            for (String resourceKey : resourcekeys) {
                Map<String, String> item = new HashMap<>();
                item.put("resourceKey", resourceKey);
                requestBodyList.add(item);
            }

            // Convertir el cuerpo a JSON
            ObjectMapper objectMapper = new ObjectMapper();
            String requestBody;
            try {
                requestBody = objectMapper.writeValueAsString(requestBodyList);
            } catch (JsonProcessingException e) {
                throw new CustomException("Error al construir el cuerpo de la solicitud",
                        "CrypterService | getFiles | error: " + e.getMessage());
            }

            if (!requestBody.isEmpty()) {
                // Crear entidad con cuerpo y encabezados
                HttpEntity<String> entity = new HttpEntity<>(requestBody, this.getHeaders());

                // Hacer la solicitud POST
                ResponseEntity<List<ImagenCrypterDTO>> response;
                try {
                    response = restTemplate.exchange(this.crypter + "/document/getFiles", HttpMethod.POST, entity, new ParameterizedTypeReference<List<ImagenCrypterDTO>>() {
                    });
                } catch (RuntimeException e) {
                    System.out.println("Error en la respuesta del servidor de imagenes: " + e.getMessage());
                    throw new CustomException("Error en la respuesta del servidor de imagenes", e.getMessage());
                }

                List<ImagenCrypterDTO> body = response.getBody();
                for (ImagenCrypterDTO dto: body) {
                    if (dto == null) {
                        throw new CustomException("Error en la respuesta del servidor de archivos", "Body es null | CrypterService | getFiles");
                    }

                    if (dto.getDest().isEmpty()) {
                        throw new CustomException("Error en la respuesta del servidor de archivos", "Falta dest | CrypterService | putFileToCrypter | body : " + dto.getJsonString());
                    }

                    respuesta.add(dto.getDest());
                }
            }

            return respuesta;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Error al intentar obtener el archivo", "CrypterService | getFiles | error: " + e.getMessage());
        }
    }

    // Obtiene un unico archivo buscando por su resourcekey
    public String getFile(String resourcekey) {
        try {
            return this.getFiles(Collections.singletonList(resourcekey)).get(0);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Error al intentar obtener el archivo", "CrypterService | getFile | error: " + e.getMessage());
        }
    }


    // Guarda un archivo de manera segura y encriptada, recibiendo unicamente el base64 del archivo
    public String putFile(String base64) {
        return putFileToCrypter(base64, true, null, null, null);
    }

    // Espera como paraemtro si se debe o no encriptar. El archivo seguira siendo privado
    // Util para información no confidencial o previews difuminadas
    // Los archivos no encriptadas son más ligeros y rapidos de consultar
    public String putFile(String base64, boolean encriptar) {
        return putFileToCrypter(base64, encriptar, null, null, null);
    }

    // El nombre de la carpeta se usa para debug
    // Puede ser el uuid, número de un expediente, número de una caja, dni de un paciente, número de una causa, etc.
    // Las carpetas no deberian contener más de 2.000 archivos en la misma carpeta, más o menos
    public String putFile(String base64, boolean encriptar, String nombreCarpeta) {
        return putFileToCrypter(base64, encriptar, nombreCarpeta, null, null);
    }

    // La coleccion se usa para debug
    // Son similares a las carpetas, pero para grupos más grandes
    // Dentro de una colleccion existen muchas carpetas
    // Una coleccion puede ser el anio de emision de un expediente, el area u oficina de donde proviene, etc.
    public String putFile(String base64, boolean encriptar, String nombreCarpeta, String nombreColeccion) {
        return putFileToCrypter(base64, encriptar, nombreCarpeta, nombreColeccion, null);
    }

    // El nombre del archivo se usa para debug
    // El nombre del archivo es el nombre que tenía el archivo original
    // Ej. Imagen.png, cargaDeDatos.pdf, etc.
    public String putFile(String base64, boolean encriptar, String nombreCarpeta, String nombreColeccion, String nombreArchivo) {
        return putFileToCrypter(base64, encriptar, nombreCarpeta, nombreColeccion, nombreArchivo);
    }

    // Recibe un archivo en base64, lo sube al webservice crypter y responde un resourceKey, con el cual luego se puede obtener
    private String putFileToCrypter(String base64, boolean encriptar, String nombreCarpeta, String nombreColeccion, String nombreArchivo) {
        validateConfiguration();
        try {
            RestTemplate restTemplate = new RestTemplate();
            LocalDate today = LocalDate.now();

            if (nombreCarpeta == null || nombreCarpeta.isEmpty()) {
                nombreCarpeta = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            }

            if (nombreColeccion == null || nombreColeccion.isEmpty()) {
                nombreColeccion = today.format(DateTimeFormatter.ofPattern("yyyy"));
            }

            if (nombreArchivo == null || nombreArchivo.isEmpty()) {
                nombreArchivo = "";
            }

            // Crear el cuerpo de la solicitud
            Map<String, Object> requestBody = new HashMap<>();
            if (encriptar) {
                requestBody.put("crypt", 1);
            } else {
                requestBody.put("crypt", 0);
            }
            requestBody.put("validation", 1);
            requestBody.put("parentKey", nombreCarpeta);
            requestBody.put("collection", nombreColeccion);
            requestBody.put("tipoDocumento", this.tipoDocumento);
            requestBody.put("name", nombreArchivo);
            requestBody.put("dataSource", base64);

            // Crear la entidad con cuerpo y encabezados
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, this.getHeaders());

            try {
                ResponseEntity<ImagenCrypterDTO> response = restTemplate.exchange(
                        this.crypter + "/document/putFile",
                        HttpMethod.POST,
                        entity,
                        new ParameterizedTypeReference<>() {
                        }
                );

                ImagenCrypterDTO body = response.getBody();

                if (body == null) {
                    throw new CustomException("Error en la respuesta del servidor de archivos", "Body es null | CrypterService | putFileToCrypter");
                }

                if (body.getResourceKey().isEmpty()) {
                    throw new CustomException("Error en la respuesta del servidor de archivos", "Falta resource key | CrypterService | putFileToCrypter | body : " + body.getJsonString());
                }

                if (body.getDest().isEmpty()) {
                    throw new CustomException("Error en la respuesta del servidor de archivos", "Falta dest | CrypterService | putFileToCrypter | body : " + body.getJsonString());
                }

                if (!body.getErrMessage().isEmpty()) {
                    throw new CustomException("Error en la respuesta del servidor de archivos", "Existe un mensaje de error | CrypterService | putFileToCrypter | body : " + body.getJsonString());
                }

                if (body.getResult() != 1) {
                    throw new CustomException("Error en la respuesta del servidor de archivos", "Result es distinto a 1 | CrypterService | putFileToCrypter | body : " + body.getJsonString());
                }

                return body.getResourceKey();
            } catch (CustomException e) {
                throw e;
            } catch (Exception e) {
                throw new CustomException("Error al llamar al servidor de archivos", "CrypterService | putFileToCrypter | response error: " + e.getMessage());
            }
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Error al intentar guardar la imagen", "CrypterService | putFileToCrypter | error: " + e.getMessage());
        }
    }
}
