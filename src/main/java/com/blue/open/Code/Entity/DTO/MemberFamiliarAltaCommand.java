package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.ParentescoEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class MemberFamiliarAltaCommand extends MemberAltaUserFields {

    @NotBlank(message = "Socio titular requerido")
    private String titularMemberUuid;

    @NotNull(message = "Parentesco requerido")
    private ParentescoEnum parentesco;

    private LocalDate fechaAlta;

    /** Solo lo usa la importación masiva: respeta el número de la planilla si está libre. */
    private Integer numeroSocio;

    /** Opcional: puede llegar null cuando el alta no asigna ningún descuento. */
    @Valid
    private List<MemberDescuentoCommand> descuentos;
}
