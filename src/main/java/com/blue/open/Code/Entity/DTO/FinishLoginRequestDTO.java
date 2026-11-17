package com.blue.open.Code.Entity.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FinishLoginRequestDTO {

    @NotBlank(message = "Código requerido")
    private String code;
}
