package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.EventoAcceso;
import com.blue.open.Code.Entity.DTO.EventoAccesoPaginateRequestDTO;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

public final class EventoAccesoSpecifications {

    private static final int STATUS_ACTIVE = 0;

    private EventoAccesoSpecifications() {
    }

    public static Specification<EventoAcceso> forCompany(Long companyId) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("company").get("id"), companyId),
                cb.equal(root.get("status"), STATUS_ACTIVE));
    }

    public static Specification<EventoAcceso> withPaginateRequest(EventoAccesoPaginateRequestDTO req) {
        return (root, query, cb) -> {
            if (req == null || req.getSearch() == null || req.getSearch().isBlank()) {
                return cb.conjunction();
            }
            String pattern = "%" + req.getSearch().trim().toLowerCase() + "%";
            return cb.like(cb.lower(root.get("titulo")), pattern);
        };
    }
}
