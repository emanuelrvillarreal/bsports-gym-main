package com.blue.open.Code.Event;

import lombok.Getter;

/**
 * Publicado tras crear o editar un rango de cuota social.
 * {@link CuotaSocialRangoCambiadoEventListener} recalcula de forma asíncrona el rango
 * asignado a los socios de la company.
 */
@Getter
public class CuotaSocialRangoCambiadoEvent {

    private final Long companyId;
    private final Long cuotaSocialRangoId;
    private final String cuotaSocialRangoUuid;

    public CuotaSocialRangoCambiadoEvent(Long companyId, Long cuotaSocialRangoId, String cuotaSocialRangoUuid) {
        this.companyId = companyId;
        this.cuotaSocialRangoId = cuotaSocialRangoId;
        this.cuotaSocialRangoUuid = cuotaSocialRangoUuid;
    }
}
