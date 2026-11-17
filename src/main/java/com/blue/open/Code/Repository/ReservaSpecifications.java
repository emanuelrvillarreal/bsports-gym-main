package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.Amenity;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.Reserva;
import com.blue.open.Code.Entity.Db.Slot;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.DTO.ReservaPaginateRequestDTO;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class ReservaSpecifications {

    private static final int STATUS_ACTIVE = 0;

    private ReservaSpecifications() {
    }

    public static Specification<Reserva> forCompany(Long companyId) {
        return (root, query, cb) -> {
            if (query != null) {
                query.distinct(true);
            }
            return cb.and(
                    cb.equal(root.get("company").get("id"), companyId),
                    cb.equal(root.get("status"), STATUS_ACTIVE));
        };
    }

    public static Specification<Reserva> withPaginateRequest(ReservaPaginateRequestDTO req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (req == null) {
                return cb.conjunction();
            }
            List<com.blue.open.Code.Entity.Enums.ReservaEstadoEnum> estados = req.getEstados();
            if (estados != null && !estados.isEmpty()) {
                predicates.add(root.get("estado").in(estados));
            }
            if (req.getAmenityUuid() != null && !req.getAmenityUuid().isBlank()) {
                Join<Reserva, Amenity> amenity = root.join("amenity", JoinType.INNER);
                predicates.add(cb.equal(amenity.get("uuid"), req.getAmenityUuid().trim()));
            }
            if (req.getMemberUuid() != null && !req.getMemberUuid().isBlank()) {
                Join<Reserva, Member> member = root.join("member", JoinType.INNER);
                predicates.add(cb.equal(member.get("uuid"), req.getMemberUuid().trim()));
            }
            if (req.getSlotDesde() != null || req.getSlotHasta() != null) {
                Join<Reserva, Slot> slot = root.join("slot", JoinType.INNER);
                if (req.getSlotDesde() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(slot.get("inicio"), req.getSlotDesde()));
                }
                if (req.getSlotHasta() != null) {
                    predicates.add(cb.lessThanOrEqualTo(slot.get("inicio"), req.getSlotHasta()));
                }
            }
            if (req.getSearch() != null && !req.getSearch().isBlank()) {
                Join<Reserva, Member> member = root.join("member", JoinType.INNER);
                Join<Member, User> user = member.join("user", JoinType.INNER);
                String pattern = "%" + req.getSearch().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(user.get("nombre")), pattern),
                        cb.like(cb.lower(user.get("apellido")), pattern)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Reserva> fetchListDetails() {
        return (root, query, cb) -> {
            if (query != null && Long.class != query.getResultType()) {
                root.fetch("amenity", JoinType.INNER);
                root.fetch("member", JoinType.INNER).fetch("user", JoinType.LEFT);
                root.fetch("slot", JoinType.INNER);
            }
            return cb.conjunction();
        };
    }
}
