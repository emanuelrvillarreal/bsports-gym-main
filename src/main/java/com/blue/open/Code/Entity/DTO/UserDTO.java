package com.blue.open.Code.Entity.DTO;

import com.blue.open.Core.Entity.DTO.BaseUserDTO;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UserDTO extends BaseUserDTO {

    private String nombre;
    private String apellido;
    private String dni;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate fechaNacimiento;
    private String imageUrl;

    @JsonProperty("telefono")
    @JsonAlias("phone")
    public String getTelefono() {
        return getPhone();
    }

    @JsonProperty("telefono")
    @JsonAlias("phone")
    public void setTelefono(String telefono) {
        setPhone(telefono);
    }
}
