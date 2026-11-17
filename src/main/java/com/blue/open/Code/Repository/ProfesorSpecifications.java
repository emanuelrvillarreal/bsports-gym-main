package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.Profesor;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.DTO.ProfesorPaginateRequestDTO;
import com.blue.open.Code.Entity.Enums.ProfesorEstadoEnum;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class ProfesorSpecifications {

    private static final int STATUS_ACTIVE = 0;

    private ProfesorSpecifications() {
    }

    public static Specification<Profesor> forCompany(Long companyId) {
        return (root, query, cb) -> {
            if (query != null) {
                query.distinct(true);
            }
            return cb.and(
                    cb.equal(root.get("company").get("id"), companyId),
                    cb.equal(root.get("status"), STATUS_ACTIVE));
        };
    }

    public static Specification<Profesor> withPaginateRequest(ProfesorPaginateRequestDTO req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (req == null) {
                return cb.conjunction();
            }
            List<ProfesorEstadoEnum> estados = req.getEstados();
            if (estados != null && !estados.isEmpty()) {
                predicates.add(root.get("estado").in(estados));
            }
            if (req.getSearch() != null && !req.getSearch().isBlank()) {
                Join<Profesor, User> user = root.join("user", JoinType.INNER);
                String pattern = "%" + req.getSearch().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(user.get("nombre")), pattern),
                        cb.like(cb.lower(user.get("apellido")), pattern),
                        cb.like(cb.lower(user.get("dni")), pattern)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Profesor> fetchUser() {
        return (root, query, cb) -> {
            if (query != null && Long.class != query.getResultType()) {
                root.fetch("user", JoinType.INNER);
            }
            return cb.conjunction();
        };
    }
}
