package com.blue.open.Code.Event;

import lombok.Getter;

/**
 * Publicado tras la baja lógica de un rango de cuota social.
 * {@link CuotaSocialRangoBajaEventListener} recalcula de forma asíncrona el rango de los
 * socios que tenían asignado el rango dado de baja.
 */
@Getter
public class CuotaSocialRangoBajaEvent {

    private final Long companyId;
    private final Long cuotaSocialRangoId;
    private final String cuotaSocialRangoUuid;

    public CuotaSocialRangoBajaEvent(Long companyId, Long cuotaSocialRangoId, String cuotaSocialRangoUuid) {
        this.companyId = companyId;
        this.cuotaSocialRangoId = cuotaSocialRangoId;
        this.cuotaSocialRangoUuid = cuotaSocialRangoUuid;
    }
}
