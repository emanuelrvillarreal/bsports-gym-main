package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.CuotaSocialRango;
import com.blue.open.Code.Repository.Custom.CuotaSocialRangoRepositoryCustom;
import com.blue.open.Core.Repository.EntityRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CuotaSocialRangoRepository extends EntityRepository<CuotaSocialRango>, CuotaSocialRangoRepositoryCustom {

    @Query("""
            SELECT r FROM CuotaSocialRango r
            WHERE r.company.id = :companyId
              AND r.status = 0
            ORDER BY r.edadHasta
            """)
    List<CuotaSocialRango> findActivosByCompanyId(@Param("companyId") Long companyId);

    @Query("""
            SELECT r FROM CuotaSocialRango r
            WHERE r.uuid = :uuid
              AND r.company.id = :companyId
              AND r.status = 0
            """)
    Optional<CuotaSocialRango> findByUuidAndCompanyIdAndStatusActive(
            @Param("uuid") String uuid,
            @Param("companyId") Long companyId);

    @Query(value = """
            SELECT * FROM cuotas_sociales_rangos r
            WHERE r.company_id = :companyId
              AND r.status = 0
              AND r.edad_hasta >= :edad
            ORDER BY r.edad_hasta ASC
            LIMIT 1
            """, nativeQuery = true)
    Optional<CuotaSocialRango> findByCompanyIdAndEdadHasta(
            @Param("companyId") Long companyId,
            @Param("edad") Integer edad);

    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM CuotaSocialRango r
            WHERE r.company.id = :companyId
              AND LOWER(r.nombre) = LOWER(:nombre)
              AND r.status = 0
              AND (:excludeId IS NULL OR r.id <> :excludeId)
            """)
    boolean existsNombreActivoEnCompany(
            @Param("companyId") Long companyId,
            @Param("nombre") String nombre,
            @Param("excludeId") Long excludeId);

    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM CuotaSocialRango r
            WHERE r.company.id = :companyId
              AND r.edadHasta = :edadHasta
              AND r.status = 0
              AND (:excludeId IS NULL OR r.id <> :excludeId)
            """)
    boolean existsEdadHastaActivoEnCompany(
            @Param("companyId") Long companyId,
            @Param("edadHasta") Integer edadHasta,
            @Param("excludeId") Long excludeId);

    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM CuotaSocialRango r
            WHERE r.company.id = :companyId
              AND r.esDefault = true
              AND r.status = 0
            """)
    boolean existsDefaultActivoEnCompany(@Param("companyId") Long companyId);
}
