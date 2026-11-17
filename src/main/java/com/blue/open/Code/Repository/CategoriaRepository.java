package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.Categoria;
import com.blue.open.Code.Repository.Custom.CategoriaRepositoryCustom;
import com.blue.open.Core.Repository.EntityRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends EntityRepository<Categoria>, CategoriaRepositoryCustom {

    @Query("""
            SELECT c FROM Categoria c
            WHERE c.company.id = :companyId
              AND c.status = 0
            ORDER BY c.nombre
            """)
    List<Categoria> findActivasByCompanyId(@Param("companyId") Long companyId);

    @Query("""
            SELECT c FROM Categoria c
            WHERE c.uuid = :uuid
              AND c.company.id = :companyId
              AND c.status = 0
            """)
    Optional<Categoria> findByUuidAndCompanyIdAndStatusActive(
            @Param("uuid") String uuid,
            @Param("companyId") Long companyId);

    @Query("""
            SELECT c FROM Categoria c
            WHERE c.uuid IN :uuids
              AND c.company.id = :companyId
              AND c.status = 0
            """)
    List<Categoria> findByUuidInAndCompanyIdAndStatusActive(
            @Param("uuids") Collection<String> uuids,
            @Param("companyId") Long companyId);

    @Query("""
            SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END
            FROM Categoria c
            WHERE c.company.id = :companyId
              AND LOWER(c.nombre) = LOWER(:nombre)
              AND c.status = 0
              AND (:excludeId IS NULL OR c.id <> :excludeId)
            """)
    boolean existsNombreActivoEnCompany(
            @Param("companyId") Long companyId,
            @Param("nombre") String nombre,
            @Param("excludeId") Long excludeId);
}
