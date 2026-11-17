package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.Categoria;
import com.blue.open.Code.Entity.DTO.CategoriaPaginateRequestDTO;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class CategoriaSpecifications {

    private static final int STATUS_ACTIVE = 0;

    private CategoriaSpecifications() {
    }

    public static Specification<Categoria> forCompany(Long companyId) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("company").get("id"), companyId),
                cb.equal(root.get("status"), STATUS_ACTIVE));
    }

    public static Specification<Categoria> withPaginateRequest(CategoriaPaginateRequestDTO req) {
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
