package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.Cuota;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.DTO.CuotaFiltroDTO;
import com.blue.open.Code.Entity.DTO.CuotaPaginateRequestDTO;
import com.blue.open.Code.Entity.Enums.CuotaEstadoEnum;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public final class CuotaSpecifications {

    private static final int STATUS_ACTIVE = 0;

    private CuotaSpecifications() {
    }

    public static Specification<Cuota> forCompany(Long companyId) {
        return (root, query, cb) -> {
            if (query != null) {
                query.distinct(true);
            }
            return cb.and(
                    cb.equal(root.get("company").get("id"), companyId),
                    cb.equal(root.get("status"), STATUS_ACTIVE));
        };
    }

    public static Specification<Cuota> withTitularMemberId(Long titularMemberId) {
        return (root, query, cb) ->
                cb.equal(root.get("titular").get("id"), titularMemberId);
    }

    public static Specification<Cuota> withPersonaMemberId(Long personaMemberId) {
        return (root, query, cb) ->
                cb.equal(root.get("persona").get("id"), personaMemberId);
    }

    public static Specification<Cuota> withFiltro(CuotaFiltroDTO filtro, LocalDate fechaCorte) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filtro == null) {
                return cb.conjunction();
            }
            if (filtro.getEstados() != null && !filtro.getEstados().isEmpty()) {
                predicates.add(root.get("estado").in(filtro.getEstados()));
            }
            if (filtro.getTipos() != null && !filtro.getTipos().isEmpty()) {
                predicates.add(root.get("tipo").in(filtro.getTipos()));
            }
            if (filtro.getUltimosMeses() != null && filtro.getUltimosMeses() > 0) {
                YearMonth desde = YearMonth.from(fechaCorte).minusMonths(filtro.getUltimosMeses() - 1L);
                predicates.add(cb.or(
                        cb.greaterThan(root.get("anio"), desde.getYear()),
                        cb.and(
                                cb.equal(root.get("anio"), desde.getYear()),
                                cb.greaterThanOrEqualTo(root.get("mes"), desde.getMonthValue()))));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Cuota> withPaginateRequest(
            CuotaPaginateRequestDTO req, Long titularId, Long personaId, LocalDate fechaCorte) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (req.getEstados() != null && !req.getEstados().isEmpty()) {
                predicates.add(root.get("estado").in(req.getEstados()));
            }
            if (req.getTipos() != null && !req.getTipos().isEmpty()) {
                predicates.add(root.get("tipo").in(req.getTipos()));
            }
            if (titularId != null) {
                predicates.add(cb.equal(root.get("titular").get("id"), titularId));
            }
            if (personaId != null) {
                predicates.add(cb.equal(root.get("persona").get("id"), personaId));
            }
            if (req.getSearch() != null && !req.getSearch().isBlank()) {
                Join<Cuota, Member> persona = root.join("persona", JoinType.INNER);
                Join<Member, User> user = persona.join("user", JoinType.INNER);
                predicates.add(MemberSpecifications.coincideBusqueda(cb, persona, user, req.getSearch()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Cuota> fetchPersonaAndTitular() {
        return (root, query, cb) -> {
            if (query != null && Long.class != query.getResultType()) {
                root.fetch("persona", JoinType.LEFT).fetch("user", JoinType.LEFT);
                root.fetch("titular", JoinType.LEFT).fetch("user", JoinType.LEFT);
            }
            return cb.conjunction();
        };
    }
}
