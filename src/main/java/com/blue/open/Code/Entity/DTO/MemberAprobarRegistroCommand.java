package com.blue.open.Code.Entity.DTO;

import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MemberAprobarRegistroCommand {

    /** Opcional: puede llegar null cuando la aprobación no asigna ningún descuento. */
    @Valid
    private List<MemberDescuentoCommand> descuentos;
}
