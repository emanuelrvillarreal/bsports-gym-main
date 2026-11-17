package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Repository.Custom.ActividadRepositoryCustom;
import com.blue.open.Core.Repository.EntityRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ActividadRepository extends EntityRepository<Actividad>, ActividadRepositoryCustom {

    List<Actividad> findByStatus(Integer status);

    @Query("""
            SELECT a FROM Actividad a
            WHERE a.uuid = :uuid
              AND a.company.id = :companyId
              AND a.status = 0
            """)
    Optional<Actividad> findByUuidAndCompany_IdAndStatus(
            @Param("uuid") String uuid,
            @Param("companyId") Long companyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT a FROM Actividad a
            WHERE a.uuid = :uuid
              AND a.company.id = :companyId
              AND a.status = 0
            """)
    Optional<Actividad> findByUuidAndCompany_IdAndStatusForUpdate(
            @Param("uuid") String uuid,
            @Param("companyId") Long companyId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT a FROM Actividad a
            WHERE a.id = :id
              AND a.company.id = :companyId
              AND a.status = 0
            """)
    Optional<Actividad> findByIdAndCompany_IdAndStatusForUpdate(
            @Param("id") Long id,
            @Param("companyId") Long companyId);

    @Query("""
            SELECT a FROM Actividad a
            WHERE a.uuid = :uuid
              AND a.company.id = :companyId
            """)
    Optional<Actividad> findByUuidAndCompany_Id(
            @Param("uuid") String uuid,
            @Param("companyId") Long companyId);

    @Query("""
            SELECT a FROM Actividad a
            LEFT JOIN FETCH a.categorias
            WHERE a.uuid = :uuid
              AND a.company.id = :companyId
              AND a.status = 0
            """)
    Optional<Actividad> findByUuidAndCompanyIdAndStatusActiveWithCategoria(
            @Param("uuid") String uuid,
            @Param("companyId") Long companyId);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            DELETE ac FROM actividades_categorias ac
            JOIN actividades a ON a.id = ac.actividad_id
            WHERE ac.categoria_id = :categoriaId
              AND a.company_id = :companyId
              AND a.status = 0
            """, nativeQuery = true)
    int clearCategoriaByCategoriaIdAndCompanyId(
            @Param("categoriaId") Long categoriaId,
            @Param("companyId") Long companyId);

    @Query("SELECT a.id FROM Actividad a WHERE a.company.id = :companyId AND a.status = 0")
    List<Long> findIdsActivasByCompanyId(@Param("companyId") Long companyId);
}
