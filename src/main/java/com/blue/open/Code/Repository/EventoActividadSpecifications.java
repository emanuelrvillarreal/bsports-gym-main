package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.DTO.EventoActividadPaginateRequestDTO;
import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.ActividadProfesor;
import com.blue.open.Code.Entity.Db.EventoActividad;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class EventoActividadSpecifications {

    private static final int STATUS_ACTIVE = 0;

    private EventoActividadSpecifications() {
    }

    public static Specification<EventoActividad> forCompany(Long companyId) {
        return (root, query, cb) -> {
            if (query != null) {
                query.distinct(true);
                if (!Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
                    root.fetch("actividad", JoinType.LEFT);
                }
            }
            return cb.and(
                    cb.equal(root.get("company").get("id"), companyId),
                    cb.equal(root.get("status"), STATUS_ACTIVE));
        };
    }

    public static Specification<EventoActividad> forProfesor(Long profesorId) {
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<ActividadProfesor> apRoot = subquery.from(ActividadProfesor.class);
            subquery.select(apRoot.get("actividadId"));
            subquery.where(cb.and(
                    cb.equal(apRoot.get("actividadId"), root.get("actividad").get("id")),
                    cb.equal(apRoot.get("profesorId"), profesorId),
                    cb.equal(apRoot.get("status"), STATUS_ACTIVE)));
            return cb.exists(subquery);
        };
    }

    public static Specification<EventoActividad> withPaginateRequest(EventoActividadPaginateRequestDTO req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (req == null) {
                return cb.conjunction();
            }
            if (req.getActividadUuid() != null && !req.getActividadUuid().isBlank()) {
                predicates.add(cb.equal(root.get("actividad").get("uuid"), req.getActividadUuid().trim()));
            }
            if (req.getSearch() != null && !req.getSearch().isBlank()) {
                Join<EventoActividad, Actividad> actividad = root.join("actividad", JoinType.LEFT);
                String pattern = "%" + req.getSearch().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("titulo")), pattern),
                        cb.like(cb.lower(actividad.get("nombre")), pattern)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
