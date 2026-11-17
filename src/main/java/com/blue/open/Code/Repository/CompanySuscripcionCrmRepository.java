package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.CompanySuscripcionCrm;
import com.blue.open.Code.Repository.Custom.CompanySuscripcionCrmRepositoryCustom;
import com.blue.open.Core.Repository.EntityRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompanySuscripcionCrmRepository extends EntityRepository<CompanySuscripcionCrm>,
        CompanySuscripcionCrmRepositoryCustom {

    Optional<CompanySuscripcionCrm> findFirstByCompanyIdAndStatus(Long companyId, int status);

    /** Trae el plan en la misma query (JOIN FETCH): MemberCupoService lo necesita apenas resuelve. */
    @Query("SELECT csc FROM CompanySuscripcionCrm csc JOIN FETCH csc.plan "
            + "WHERE csc.company.id = :companyId AND csc.status = :status")
    Optional<CompanySuscripcionCrm> findFirstByCompanyIdWithPlan(
            @Param("companyId") Long companyId, @Param("status") int status);

    /**
     * Version en lote de findFirstByCompanyIdWithPlan, para paginados que necesitan la
     * suscripcion de varios clubes de una sola vez (evita N+1).
     */
    @Query("SELECT csc FROM CompanySuscripcionCrm csc JOIN FETCH csc.plan "
            + "WHERE csc.company.id IN :companyIds AND csc.status = :status")
    List<CompanySuscripcionCrm> findByCompanyIdInWithPlan(
            @Param("companyIds") List<Long> companyIds, @Param("status") int status);
}
