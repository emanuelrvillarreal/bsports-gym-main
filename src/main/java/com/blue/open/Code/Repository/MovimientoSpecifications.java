package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.Movimiento;
import com.blue.open.Code.Entity.DTO.MovimientoPaginateRequestDTO;
import com.blue.open.Code.Entity.Enums.MovimientoEstadoEnum;
import com.blue.open.Code.Entity.Enums.MovimientoTipoEnum;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class MovimientoSpecifications {

    private static final int STATUS_ACTIVE = 0;

    private MovimientoSpecifications() {
    }

    public static Specification<Movimiento> forCompany(Long companyId) {
        return (root, query, cb) -> {
            if (query != null) {
                query.distinct(true);
            }
            return cb.and(
                    cb.equal(root.get("company").get("id"), companyId),
                    cb.equal(root.get("status"), STATUS_ACTIVE));
        };
    }

    public static Specification<Movimiento> withPaginateRequest(MovimientoPaginateRequestDTO req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (req.getFechaDesde() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fecha"), req.getFechaDesde()));
            }
            if (req.getFechaHasta() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fecha"), req.getFechaHasta()));
            }

            if (req.getTipo() != null) {
                predicates.add(cb.equal(root.get("tipo"), req.getTipo()));
            }
            List<MovimientoEstadoEnum> estados = req.getEstados();
            if (estados != null && !estados.isEmpty()) {
                predicates.add(root.get("estado").in(estados));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Movimiento> withTipoAndFechaBetween(
            MovimientoTipoEnum tipo,
            MovimientoEstadoEnum estado,
            LocalDate desde,
            LocalDate hasta) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("tipo"), tipo),
                cb.equal(root.get("estado"), estado),
                cb.between(root.get("fecha"), desde, hasta));
    }

    public static Specification<Movimiento> fetchListDetails() {
        return (root, query, cb) -> {
            if (query != null && Long.class != query.getResultType()) {
                root.fetch("cuota", JoinType.LEFT);
                root.fetch("persona", JoinType.LEFT).fetch("user", JoinType.LEFT);
                root.fetch("titular", JoinType.LEFT);
                root.fetch("registradoPor", JoinType.LEFT);
            }
            return cb.conjunction();
        };
    }

    public static Specification<Movimiento> fetchDetail() {
        return (root, query, cb) -> {
            if (query != null && Long.class != query.getResultType()) {
                root.fetch("cuota", JoinType.LEFT);
                root.fetch("persona", JoinType.LEFT).fetch("user", JoinType.LEFT);
                root.fetch("titular", JoinType.LEFT).fetch("user", JoinType.LEFT);
                root.fetch("registradoPor", JoinType.LEFT);
                root.fetch("anuladaPor", JoinType.LEFT);
            }
            return cb.conjunction();
        };
    }
}
