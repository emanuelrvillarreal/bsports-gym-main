package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.CuotaSocialRango;
import com.blue.open.Code.Entity.DTO.CuotaSocialRangoPaginateRequestDTO;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class CuotaSocialRangoSpecifications {

    private static final int STATUS_ACTIVE = 0;

    private CuotaSocialRangoSpecifications() {
    }

    public static Specification<CuotaSocialRango> forCompany(Long companyId) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("company").get("id"), companyId),
                cb.equal(root.get("status"), STATUS_ACTIVE));
    }

    public static Specification<CuotaSocialRango> withPaginateRequest(CuotaSocialRangoPaginateRequestDTO req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (req == null || req.getSearch() == null || req.getSearch().isBlank()) {
                return cb.conjunction();
            }
            String pattern = "%" + req.getSearch().trim().toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("nombre")), pattern),
                    cb.like(cb.lower(root.get("descripcion")), pattern)));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
