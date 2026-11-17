package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.AccesoClub;
import com.blue.open.Code.Repository.Custom.AccesoClubRepositoryCustom;
import com.blue.open.Core.Repository.EntityRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AccesoClubRepository extends EntityRepository<AccesoClub>, AccesoClubRepositoryCustom {

    @Query("""
            SELECT a FROM AccesoClub a
            JOIN FETCH a.user
            WHERE a.company.id = :companyId
              AND a.status = 0
              AND a.fecha >= :desde
              AND a.fecha <= :hasta
            ORDER BY a.fecha DESC
            """)
    List<AccesoClub> findByCompanyIdAndFechaBetweenWithUser(
            @Param("companyId") Long companyId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}
