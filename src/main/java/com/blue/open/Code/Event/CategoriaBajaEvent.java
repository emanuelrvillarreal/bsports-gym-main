package com.blue.open.Code.Event;

import lombok.Getter;

/**
 * Publicado tras la baja lógica de una categoría.
 * {@link CategoriaBajaEventListener} desasigna la categoría en actividades y socios de forma asíncrona.
 */
@Getter
public class CategoriaBajaEvent {

    private final Long companyId;
    private final Long categoriaId;
    private final String categoriaUuid;

    public CategoriaBajaEvent(Long companyId, Long categoriaId, String categoriaUuid) {
        this.companyId = companyId;
        this.categoriaId = categoriaId;
        this.categoriaUuid = categoriaUuid;
    }
}
