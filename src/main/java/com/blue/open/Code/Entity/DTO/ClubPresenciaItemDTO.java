package com.blue.open.Code.Entity.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ClubPresenciaItemDTO {

    private String userUuid;
    private String nombre;
    private String apellido;
    private String dni;
    private boolean presente;
    private String imageUrl;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime ultimoAccesoAt;
}
