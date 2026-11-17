package com.blue.open.Code.Entity.DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class MemberTitularAltaCommand extends MemberAltaUserFields {

    private LocalDate fechaAlta;

    /** Opcional (solo alta desde el backoffice): si no llega, se asigna el siguiente libre. */
    @Positive(message = "El número de socio debe ser mayor a 0")
    private Integer numeroSocio;

    /** Opcional: puede llegar null cuando el alta no asigna ningún descuento. */
    @Valid
    private List<MemberDescuentoCommand> descuentos;
}
