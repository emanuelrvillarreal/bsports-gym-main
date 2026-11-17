package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.AsistenciaActividad;
import com.blue.open.Code.Entity.DTO.AsistenciaPaginateRequestDTO;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class AsistenciaActividadSpecifications {

    private static final int STATUS_ACTIVE = 0;

    private AsistenciaActividadSpecifications() {
    }

    public static Specification<AsistenciaActividad> forCompany(Long companyId) {
        return (root, query, cb) -> {
            if (query != null) {
                query.distinct(true);
            }
            return cb.and(
                    cb.equal(root.get("company").get("id"), companyId),
                    cb.equal(root.get("status"), STATUS_ACTIVE));
        };
    }

    public static Specification<AsistenciaActividad> forActividadId(Long actividadId) {
        return (root, query, cb) -> cb.equal(root.get("actividad").get("id"), actividadId);
    }

    public static Specification<AsistenciaActividad> withPaginateRequest(
            AsistenciaPaginateRequestDTO req, Long memberId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (req == null) {
                return cb.conjunction();
            }

            LocalDate fechaDesde = req.getFechaDesde();
            LocalDate fechaHasta = req.getFechaHasta();
            if (fechaDesde != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fecha"), fechaDesde));
            }
            if (fechaHasta != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fecha"), fechaHasta));
            }
            if (memberId != null) {
                predicates.add(cb.equal(root.get("member").get("id"), memberId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<AsistenciaActividad> fetchConsultaDetails() {
        return (root, query, cb) -> {
            if (query != null && Long.class != query.getResultType()) {
                root.fetch("slot", JoinType.INNER);
                root.fetch("actividad", JoinType.INNER);
                var member = root.fetch("member", JoinType.INNER);
                member.fetch("user", JoinType.LEFT);
            }
            return cb.conjunction();
        };
    }
}
