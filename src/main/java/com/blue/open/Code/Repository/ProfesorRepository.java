package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.Profesor;
import com.blue.open.Code.Repository.Custom.ProfesorRepositoryCustom;
import com.blue.open.Core.Repository.EntityRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProfesorRepository extends EntityRepository<Profesor>, ProfesorRepositoryCustom {

    Optional<Profesor> findFirstByCompany_IdAndUser_Id(Long companyId, Long userId);

    @Query("SELECT p FROM Profesor p JOIN FETCH p.user WHERE p.id = :id")
    Optional<Profesor> findByIdWithUser(@Param("id") Long id);

    @Query("""
            SELECT p FROM Profesor p
            JOIN FETCH p.user
            WHERE p.uuid = :uuid
              AND p.company.id = :companyId
              AND p.status = 0
            """)
            Optional<Profesor> findByUuidAndCompanyIdWithUser(
            @Param("uuid") String uuid,
            @Param("companyId") Long companyId);

    @Query("""
            SELECT p FROM Profesor p
            JOIN FETCH p.user
            WHERE p.user.id = :userId
              AND p.company.id = :companyId
              AND p.status = 0
            """)
    Optional<Profesor> findByUserIdAndCompanyIdWithUser(
            @Param("userId") Long userId,
            @Param("companyId") Long companyId);

    @Query("""
            SELECT p FROM Profesor p
            WHERE p.uuid = :uuid
              AND p.company.id = :companyId
            """)
    Optional<Profesor> findByUuidAndCompanyId(
            @Param("uuid") String uuid,
            @Param("companyId") Long companyId);

    @Query("""
            SELECT p FROM Profesor p
            JOIN FETCH p.user
            WHERE p.id IN :ids
              AND p.status = 0
            """)
    List<Profesor> findByIdInWithUser(@Param("ids") List<Long> ids);

    @Query("SELECT p FROM Profesor p JOIN FETCH p.user WHERE p.id IN :ids")
    List<Profesor> findAllWithUserByIdIn(@Param("ids") java.util.Collection<Long> ids);
}
