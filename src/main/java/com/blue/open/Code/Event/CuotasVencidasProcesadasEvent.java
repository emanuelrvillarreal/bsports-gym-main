package com.blue.open.Code.Event;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class CuotasVencidasProcesadasEvent {

    private final LocalDate fechaCorte;
    private final int companiesProcessed;
    private final int cuotasMarcadasVencidas;

    public CuotasVencidasProcesadasEvent(
            LocalDate fechaCorte,
            int companiesProcessed,
            int cuotasMarcadasVencidas) {
        this.fechaCorte = fechaCorte;
        this.companiesProcessed = companiesProcessed;
        this.cuotasMarcadasVencidas = cuotasMarcadasVencidas;
    }
}
