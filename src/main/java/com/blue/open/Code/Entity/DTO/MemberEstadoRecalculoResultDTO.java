package com.blue.open.Code.Entity.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberEstadoRecalculoResultDTO {

    private int gruposProcesados;
    private int miembrosActualizados;
    private int inhabilitaciones;
    private int recuperaciones;

    public void merge(MemberEstadoRecalculoResultDTO other) {
        if (other == null) {
            return;
        }
        this.gruposProcesados += other.gruposProcesados;
        this.miembrosActualizados += other.miembrosActualizados;
        this.inhabilitaciones += other.inhabilitaciones;
        this.recuperaciones += other.recuperaciones;
    }

    @Override
    public String toString() {
        return "MemberEstadoRecalculoResult{grupos=" + gruposProcesados
                + ", actualizados=" + miembrosActualizados
                + ", inhabilitaciones=" + inhabilitaciones
                + ", recuperaciones=" + recuperaciones + "}";
    }
}
