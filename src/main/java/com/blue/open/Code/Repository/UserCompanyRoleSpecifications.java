package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Core.Entity.Db.Role;
import com.blue.open.Core.Entity.Db.UserCompanyRole;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public final class UserCompanyRoleSpecifications {

    private static final int STATUS_ACTIVE = 0;

    private UserCompanyRoleSpecifications() {
    }

    public static Specification<UserCompanyRole> forCompanyAndRole(Long companyId, String roleName) {
        return (root, query, cb) -> {
            if (query != null) {
                query.distinct(true);
            }
            Join<UserCompanyRole, Role> role = root.join("roles", JoinType.INNER);
            return cb.and(
                    cb.equal(root.get("company").get("id"), companyId),
                    cb.equal(role.get("name"), roleName),
                    cb.equal(root.get("status"), STATUS_ACTIVE));
        };
    }

    public static Specification<UserCompanyRole> withSearch(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return cb.conjunction();
            }
            Join<UserCompanyRole, User> user = root.join("user", JoinType.INNER);
            String pattern = "%" + search.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(user.get("nombre")), pattern),
                    cb.like(cb.lower(user.get("apellido")), pattern),
                    cb.like(cb.lower(user.get("email")), pattern));
        };
    }

    public static Specification<UserCompanyRole> fetchUser() {
        return (root, query, cb) -> {
            if (query != null && Long.class != query.getResultType()) {
                root.fetch("user", JoinType.INNER);
            }
            return cb.conjunction();
        };
    }
}
