package com.blue.open.Code.Entity.DTO;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class AsistenciaPaginateRequestDTO {

    private int page = 1;
    private int size = 20;

    @NotBlank(message = "Actividad requerida")
    private String actividadUuid;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate fechaDesde;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate fechaHasta;

    @JsonAlias("personaMemberUuid")
    private String memberUuid;
}
