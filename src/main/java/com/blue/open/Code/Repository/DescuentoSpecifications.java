package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.DTO.DescuentoPaginateRequestDTO;
import com.blue.open.Code.Entity.Db.Descuento;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class DescuentoSpecifications {

    private static final int STATUS_ACTIVE = 0;

    private DescuentoSpecifications() {
    }

    public static Specification<Descuento> forCompany(Long companyId) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("company").get("id"), companyId),
                cb.equal(root.get("status"), STATUS_ACTIVE));
    }

    public static Specification<Descuento> withPaginateRequest(DescuentoPaginateRequestDTO req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (req == null || req.getSearch() == null || req.getSearch().isBlank()) {
                return cb.conjunction();
            }
            String pattern = "%" + req.getSearch().trim().toLowerCase() + "%";
            predicates.add(cb.like(cb.lower(root.get("titulo")), pattern));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
