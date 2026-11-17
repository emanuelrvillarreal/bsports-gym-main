package com.blue.open.Code.Entity.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StartLoginRequestDTO {

    @NotBlank(message = "Email requerido")
    @Email(message = "Email inválido")
    private String email;

    // Path del club, para los colores y el logo del mail. Opcional
    private String path;
}
