package com.blue.open.Code.Entity.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class MovimientoDetalleDTO extends MovimientoRowDTO {

    private String anuladaPorUuid;
    private String anuladaMotivo;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime anuladaAt;
}
