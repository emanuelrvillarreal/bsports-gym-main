package com.blue.open.Core.Repository.Impl;

import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Core.Entity.Db.Role;
import com.blue.open.Core.Entity.Db.UserCompanyRole;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Custom.UserRepositoryCustom;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserRepositoryImpl extends RepositoryImpl<User> implements UserRepositoryCustom<User> {
    @Override
    public Page<User> paginate(PaginateList paginateList) {
        return createSql(paginateList);
    }

    @Override
    @Transactional
    public User findFirstByUuidWithDetails(String uuid) {
        List<User> users = entityManager.createQuery(
                "SELECT DISTINCT u FROM User u " +
                        "LEFT JOIN FETCH u.userCompanyRoles ucr " +
                        "LEFT JOIN FETCH ucr.company " +
                        "WHERE u.uuid = :uuid",
                User.class)
                .setParameter("uuid", uuid)
                .getResultList();

        if (users.isEmpty()) {
            return null;
        }

        entityManager.createQuery(
                "SELECT DISTINCT ucr FROM UserCompanyRole ucr " +
                        "JOIN FETCH ucr.roles " +
                        "WHERE ucr.user.uuid = :uuid",
                UserCompanyRole.class)
                .setParameter("uuid", uuid)
                .getResultList();

        entityManager.createQuery(
                "SELECT DISTINCT r FROM Role r " +
                        "JOIN FETCH r.permissions " +
                        "WHERE r IN (SELECT role FROM UserCompanyRole ucr JOIN ucr.roles role WHERE ucr.user.uuid = :uuid)",
                Role.class)
                .setParameter("uuid", uuid)
                .getResultList();

        return users.get(0);
    }
}
