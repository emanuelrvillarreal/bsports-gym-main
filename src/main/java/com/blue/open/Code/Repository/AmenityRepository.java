package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.Amenity;
import com.blue.open.Code.Repository.Custom.AmenityRepositoryCustom;
import com.blue.open.Core.Repository.EntityRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AmenityRepository extends EntityRepository<Amenity>, AmenityRepositoryCustom {

    @Query("""
            SELECT a FROM Amenity a
            WHERE a.company.id = :companyId
              AND a.status = 0
            ORDER BY a.nombre
            """)
    List<Amenity> findActivasByCompanyId(@Param("companyId") Long companyId);

    @Query("""
            SELECT a FROM Amenity a
            WHERE a.uuid = :uuid
              AND a.company.id = :companyId
              AND a.status = 0
            """)
    Optional<Amenity> findByUuidAndCompanyIdAndStatusActive(
            @Param("uuid") String uuid,
            @Param("companyId") Long companyId);
}
