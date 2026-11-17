package com.blue.open.Code.Entity.DTO;

import lombok.Getter;

/** Resultado de una corrida de SuscripcionVerificacionService. */
@Getter
public class SuscripcionVerificacionResumenDTO {

    private int facturasVencidas;
    private int clubesEvaluados;
    private int clubesBloqueados;
    private int clubesDesbloqueados;
    private int erroresCrm;

    public void setFacturasVencidas(int facturasVencidas) {
        this.facturasVencidas = facturasVencidas;
    }

    public void sumarEvaluado() {
        this.clubesEvaluados++;
    }

    public void sumarBloqueado() {
        this.clubesBloqueados++;
    }

    public void sumarDesbloqueado() {
        this.clubesDesbloqueados++;
    }

    public void sumarError() {
        this.erroresCrm++;
    }
}
