package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.ActividadProfesor;
import com.blue.open.Code.Entity.Db.Profesor;
import com.blue.open.Code.Entity.DTO.ActividadPaginateRequestDTO;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class ActividadSpecifications {

    private static final int STATUS_ACTIVE = 0;

    private ActividadSpecifications() {
    }

    public static Specification<Actividad> forCompany(Long companyId) {
        return (root, query, cb) -> {
            if (query != null) {
                query.distinct(true);
            }
            return cb.and(
                    cb.equal(root.get("company").get("id"), companyId),
                    cb.equal(root.get("status"), STATUS_ACTIVE));
        };
    }

    public static Specification<Actividad> withPaginateRequest(ActividadPaginateRequestDTO req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (req == null) {
                return cb.conjunction();
            }
            if (req.getCompleta() != null) {
                predicates.add(cb.equal(root.get("completa"), req.getCompleta()));
            }
            if (req.getSearch() != null && !req.getSearch().isBlank()) {
                String pattern = "%" + req.getSearch().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("nombre")), pattern),
                        cb.like(cb.lower(root.get("ubicacion")), pattern)));
            }
            if (req.getProfesorUuid() != null && !req.getProfesorUuid().isBlank()) {
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<ActividadProfesor> apRoot = subquery.from(ActividadProfesor.class);
                Root<Profesor> profesorRoot = subquery.from(Profesor.class);
                subquery.select(apRoot.get("actividadId"));
                subquery.where(cb.and(
                        cb.equal(apRoot.get("actividadId"), root.get("id")),
                        cb.equal(apRoot.get("profesorId"), profesorRoot.get("id")),
                        cb.equal(apRoot.get("status"), STATUS_ACTIVE),
                        cb.equal(profesorRoot.get("uuid"), req.getProfesorUuid()),
                        cb.equal(profesorRoot.get("status"), STATUS_ACTIVE)));
                predicates.add(cb.exists(subquery));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
