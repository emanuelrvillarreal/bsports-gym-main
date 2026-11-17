package com.blue.open.Code.Entity.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CuotaVencimientoResultDTO {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate fechaCorte;
    private int companiesProcessed;
    private int cuotasMarcadasVencidas;

    @Override
    public String toString() {
        return "CuotaVencimientoResult{fechaCorte=" + fechaCorte
                + ", companies=" + companiesProcessed
                + ", marcadas=" + cuotasMarcadasVencidas + "}";
    }
}
