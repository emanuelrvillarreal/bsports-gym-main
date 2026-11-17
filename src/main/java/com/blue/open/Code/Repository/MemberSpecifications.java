package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.DTO.MemberPaginateRequestDTO;
import com.blue.open.Code.Entity.Enums.MemberEstadoEnum;
import com.blue.open.Code.Service.MemberStatusConstants;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class MemberSpecifications {

    private static final int STATUS_ACTIVE = 0;

    private MemberSpecifications() {
    }

    public static Specification<Member> forCompany(Long companyId) {
        return (root, query, cb) -> {
            if (query != null) {
                query.distinct(true);
            }
            return cb.and(
                    cb.equal(root.get("company").get("id"), companyId),
                    cb.equal(root.get("status"), STATUS_ACTIVE));
        };
    }

    public static Specification<Member> forCompanyPendingApproval(Long companyId) {
        return (root, query, cb) -> {
            if (query != null) {
                query.distinct(true);
            }
            return cb.and(
                    cb.equal(root.get("company").get("id"), companyId),
                    cb.equal(root.get("status"), MemberStatusConstants.STATUS_PENDING_APPROVAL));
        };
    }

    /**
     * Búsqueda de personas para listados (socios, cuotas): por nombre, apellido, "nombre apellido" (en cualquier
     * orden), DNI o número de socio. Cada palabra tiene que aparecer en alguno de esos datos; así "juan perez",
     * "perez juan", "51" o "30111222" encuentran a la persona. Sin palabras devuelve {@code null}.
     */
    public static Predicate coincideBusqueda(CriteriaBuilder cb, From<?, Member> member, From<?, User> user,
                                             String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        List<Predicate> porPalabra = new ArrayList<>();
        for (String palabra : search.trim().toLowerCase().split("\\s+")) {
            // '!' como escape: un "%" o "_" escrito por el usuario se busca literal.
            String pattern = "%" + palabra.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            porPalabra.add(cb.or(
                    cb.like(cb.lower(user.get("nombre")), pattern, '!'),
                    cb.like(cb.lower(user.get("apellido")), pattern, '!'),
                    cb.like(cb.lower(user.get("dni")), pattern, '!'),
                    cb.like(cb.lower(member.get("numeroSocio")), pattern, '!')));
        }
        return cb.and(porPalabra.toArray(new Predicate[0]));
    }

    public static Specification<Member> withSearch(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return cb.conjunction();
            }
            Join<Member, User> user = root.join("user", JoinType.INNER);
            String pattern = "%" + search.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(user.get("nombre")), pattern),
                    cb.like(cb.lower(user.get("apellido")), pattern),
                    cb.like(cb.lower(user.get("dni")), pattern),
                    cb.like(cb.lower(user.get("email")), pattern));
        };
    }

    public static Specification<Member> withPaginateRequest(MemberPaginateRequestDTO req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (req == null) {
                return cb.conjunction();
            }
            List<MemberEstadoEnum> estados = req.getEstados();
            if (estados != null && !estados.isEmpty()) {
                predicates.add(root.get("estado").in(estados));
            }
            if (req.getSearch() != null && !req.getSearch().isBlank()) {
                Join<Member, User> user = root.join("user", JoinType.INNER);
                predicates.add(coincideBusqueda(cb, root, user, req.getSearch()));
            }
            if (req.isTitular()) {
                predicates.add(cb.isNull(root.get("titular")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** Igual a {@link #withSearch}, pero además busca por {@code numeroSocio}. */
    public static Specification<Member> withSearchIncludingNumeroSocio(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return cb.conjunction();
            }
            Join<Member, User> user = root.join("user", JoinType.INNER);
            String pattern = "%" + search.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(user.get("nombre")), pattern),
                    cb.like(cb.lower(user.get("apellido")), pattern),
                    cb.like(cb.lower(user.get("dni")), pattern),
                    cb.like(cb.lower(root.get("numeroSocio")), pattern));
        };
    }

    public static Specification<Member> fetchUserAndTitular() {
        return (root, query, cb) -> {
            if (query != null && Long.class != query.getResultType()) {
                root.fetch("user", JoinType.INNER);
                root.fetch("titular", JoinType.LEFT);
            }
            return cb.conjunction();
        };
    }
}
