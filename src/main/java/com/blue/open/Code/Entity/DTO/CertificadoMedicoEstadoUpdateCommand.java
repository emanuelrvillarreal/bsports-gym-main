package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.CertificadoMedicoEstadoEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CertificadoMedicoEstadoUpdateCommand {

    @NotNull(message = "El estado es requerido")
    private CertificadoMedicoEstadoEnum estado;
}
