package com.blue.open.Code.Entity.DTO;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberRegistroRechazoCommand {

    @Size(max = 512, message = "El motivo no puede superar 512 caracteres")
    private String motivo;
}
