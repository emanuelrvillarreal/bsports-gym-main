package com.blue.open.Core.Repository;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Core.Entity.Db.UserCompanyRole;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserCompanyRoleRepository extends EntityRepository<UserCompanyRole> {

    /** Asignaciones activas de un club para un rol determinado (por nombre, ej. "ADMIN"). */
    @Query("SELECT ucr FROM UserCompanyRole ucr JOIN ucr.roles r "
            + "WHERE ucr.company = :company AND r.name = :roleName AND ucr.status = 0")
    List<UserCompanyRole> findByCompanyAndRoleName(@Param("company") Company company, @Param("roleName") String roleName);

    /** Resuelve la asignación de un usuario puntual (por uuid) para un rol determinado en un club. */
    @Query("SELECT ucr FROM UserCompanyRole ucr JOIN FETCH ucr.user u JOIN ucr.roles r "
            + "WHERE ucr.company.id = :companyId AND u.uuid = :userUuid AND r.name = :roleName AND ucr.status = 0")
    Optional<UserCompanyRole> findByCompanyIdAndUserUuidAndRoleName(@Param("companyId") Long companyId,
            @Param("userUuid") String userUuid, @Param("roleName") String roleName);
}
