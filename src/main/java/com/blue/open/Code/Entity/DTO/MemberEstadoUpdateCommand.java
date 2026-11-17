package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.MemberEstadoEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberEstadoUpdateCommand {

    @NotNull(message = "El estado es requerido")
    private MemberEstadoEnum estado;
}
