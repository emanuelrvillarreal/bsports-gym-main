package com.blue.open.Code.Service.support;

import com.blue.open.Code.Entity.DTO.CuotaActividadAltaResultDTO;
import com.blue.open.Code.Service.Tesoreria.CuotaActividadGeneracionService;

import java.time.LocalDate;

/**
 * Doble de prueba que registra los argumentos con los que la inscripción pide la cuota,
 * sin tocar repositorios.
 */
public class RecordingCuotaActividadGeneracionService extends CuotaActividadGeneracionService {

    private boolean called;
    private Long lastMemberId;
    private Long lastActividadId;
    private Long lastInscripcionActividadId;
    private LocalDate lastFecha;
    private CuotaActividadAltaResultDTO resultToReturn = defaultGenerada();

    @Override
    public CuotaActividadAltaResultDTO generarPorInscripcion(
            Long memberId, Long actividadId, Long inscripcionActividadId, LocalDate fechaInscripcion) {
        this.called = true;
        this.lastMemberId = memberId;
        this.lastActividadId = actividadId;
        this.lastInscripcionActividadId = inscripcionActividadId;
        this.lastFecha = fechaInscripcion;
        return resultToReturn;
    }

    public void setResultToReturn(CuotaActividadAltaResultDTO resultToReturn) {
        this.resultToReturn = resultToReturn != null ? resultToReturn : defaultGenerada();
    }

    public boolean wasCalled() {
        return called;
    }

    public Long getLastMemberId() {
        return lastMemberId;
    }

    public Long getLastActividadId() {
        return lastActividadId;
    }

    public Long getLastInscripcionActividadId() {
        return lastInscripcionActividadId;
    }

    public LocalDate getLastFecha() {
        return lastFecha;
    }

    private static CuotaActividadAltaResultDTO defaultGenerada() {
        CuotaActividadAltaResultDTO result = new CuotaActividadAltaResultDTO();
        result.setGenerada(true);
        result.setCuotaUuid("cuota-uuid");
        return result;
    }

    public void reset() {
        called = false;
        lastMemberId = null;
        lastActividadId = null;
        lastInscripcionActividadId = null;
        lastFecha = null;
        resultToReturn = defaultGenerada();
    }
}
