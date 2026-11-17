package com.blue.open.Code.Service.support;

import com.blue.open.Code.Entity.DTO.CuotaSocialAltaResultDTO;
import com.blue.open.Code.Service.Tesoreria.CuotaSocialGeneracionService;

import java.time.LocalDate;

/**
 * Doble de prueba que registra los argumentos con los que el alta pide la cuota social, sin
 * tocar repositorios. Se usa una subclase real en vez de un mock de Mockito porque este
 * entorno no puede generar bytecode para las clases de generación de cuotas (ByteBuddy falla
 * con "Could not create type" bajo JDK 25); el mismo patrón ya existía para
 * {@code CuotaActividadGeneracionService}.
 */
public class RecordingCuotaSocialGeneracionService extends CuotaSocialGeneracionService {

    private boolean called;
    private Long lastMemberId;
    private LocalDate lastFechaAlta;
    private CuotaSocialAltaResultDTO resultToReturn = defaultGenerada();

    @Override
    public CuotaSocialAltaResultDTO generarPorAlta(Long memberId, LocalDate fechaAlta) {
        this.called = true;
        this.lastMemberId = memberId;
        this.lastFechaAlta = fechaAlta;
        return resultToReturn;
    }

    public void setResultToReturn(CuotaSocialAltaResultDTO resultToReturn) {
        this.resultToReturn = resultToReturn != null ? resultToReturn : defaultGenerada();
    }

    public boolean wasCalled() {
        return called;
    }

    public Long getLastMemberId() {
        return lastMemberId;
    }

    public LocalDate getLastFechaAlta() {
        return lastFechaAlta;
    }

    private static CuotaSocialAltaResultDTO defaultGenerada() {
        CuotaSocialAltaResultDTO result = new CuotaSocialAltaResultDTO();
        result.setGenerada(true);
        result.setCuotaUuid("cuota-social-uuid");
        return result;
    }

    public void reset() {
        called = false;
        lastMemberId = null;
        lastFechaAlta = null;
        resultToReturn = defaultGenerada();
    }
}
