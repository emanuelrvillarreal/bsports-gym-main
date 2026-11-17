package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.DTO.EventoAccesoHistorialPaginateRequestDTO;
import com.blue.open.Code.Entity.Db.EventoAccesoHistorial;
import com.blue.open.Code.Entity.Enums.EventoAccesoScanEstadoEnum;
import org.springframework.data.jpa.domain.Specification;

public final class EventoAccesoHistorialSpecifications {

    private static final int STATUS_ACTIVE = 0;

    private EventoAccesoHistorialSpecifications() {
    }

    public static Specification<EventoAccesoHistorial> forEvento(Long eventoAccesoId) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("eventoAcceso").get("id"), eventoAccesoId),
                cb.equal(root.get("status"), STATUS_ACTIVE));
    }

    public static Specification<EventoAccesoHistorial> withPaginateRequest(EventoAccesoHistorialPaginateRequestDTO req) {
        return (root, query, cb) -> {
            if (req == null || req.getSearch() == null || req.getSearch().isBlank()) {
                return cb.conjunction();
            }
            String pattern = "%" + req.getSearch().trim().toLowerCase() + "%";
            return cb.like(cb.lower(root.get("nombre")), pattern);
        };
    }

    public static Specification<EventoAccesoHistorial> withResultado(EventoAccesoScanEstadoEnum resultado) {
        return (root, query, cb) -> {
            if (resultado == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("resultado"), resultado);
        };
    }
}
