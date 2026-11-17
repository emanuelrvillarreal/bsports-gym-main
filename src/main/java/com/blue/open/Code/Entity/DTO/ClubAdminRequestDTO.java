package com.blue.open.Code.Entity.DTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClubAdminRequestDTO {

    @NotBlank(message = "Nombre del admin inicial es obligatorio.")
    private String nombre;

    @NotBlank(message = "Apellido del admin inicial es obligatorio.")
    private String apellido;

    @NotBlank(message = "Email del admin inicial es obligatorio.")
    @Email(message = "Email del admin inicial inválido.")
    private String email;

    @NotBlank(message = "La contraseña del administrador inicial es obligatoria.")
    private String password;
}
