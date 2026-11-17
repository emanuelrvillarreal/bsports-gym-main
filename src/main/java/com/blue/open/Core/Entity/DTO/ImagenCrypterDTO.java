package com.blue.open.Core.Entity.DTO;

import com.blue.open.Core.Exceptions.CustomException;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ImagenCrypterDTO {
    private Long id;
    private Long idLote;
    private Integer operation;
    private String name;
    private String parentKey;
    private String resourceKey;
    private String source;
    private String dest;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LocalDateTime timeInit;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private LocalDateTime timeEnd;

    private Long idCrypt;
    private Integer result;
    private String errMessage;
    private Long duration;
    private Long fileSize;
    private Integer validation;

    public String getJsonString() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new CustomException("Error al procesar la respuesta del sistema de archivos", "ImagenCrypterDTO | getJsonString | error: " + e.getMessage());
        }
    }
}
