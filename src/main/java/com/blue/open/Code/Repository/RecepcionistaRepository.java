package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.Recepcionista;
import com.blue.open.Code.Repository.Custom.RecepcionistaRepositoryCustom;
import com.blue.open.Core.Repository.EntityRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RecepcionistaRepository extends EntityRepository<Recepcionista>, RecepcionistaRepositoryCustom {

    Optional<Recepcionista> findFirstByCompany_IdAndUser_Id(Long companyId, Long userId);

    @Query("SELECT r FROM Recepcionista r JOIN FETCH r.user WHERE r.id = :id")
    Optional<Recepcionista> findByIdWithUser(@Param("id") Long id);

    @Query("""
            SELECT r FROM Recepcionista r
            JOIN FETCH r.user
            WHERE r.uuid = :uuid
              AND r.company.id = :companyId
              AND r.status = 0
            """)
    Optional<Recepcionista> findByUuidAndCompanyIdWithUser(
            @Param("uuid") String uuid,
            @Param("companyId") Long companyId);

    @Query("""
            SELECT r FROM Recepcionista r
            WHERE r.uuid = :uuid
              AND r.company.id = :companyId
            """)
    Optional<Recepcionista> findByUuidAndCompanyId(
            @Param("uuid") String uuid,
            @Param("companyId") Long companyId);

    @Query("""
            SELECT r FROM Recepcionista r
            JOIN FETCH r.user
            WHERE r.user.id = :userId
              AND r.company.id = :companyId
              AND r.status = 0
            """)
    Optional<Recepcionista> findByUserIdAndCompanyIdWithUser(
            @Param("userId") Long userId,
            @Param("companyId") Long companyId);
}
