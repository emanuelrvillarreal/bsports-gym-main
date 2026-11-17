package com.blue.open.Code.Entity.DTO;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class RecepcionistaAltaCommand extends MemberAltaUserFields {

    private LocalDate fechaAlta;
}
