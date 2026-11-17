package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.CertificadoMedico;
import com.blue.open.Code.Entity.Enums.CertificadoMedicoEstadoEnum;
import com.blue.open.Code.Repository.Custom.CertificadoMedicoRepositoryCustom;
import com.blue.open.Core.Repository.EntityRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface CertificadoMedicoRepository extends EntityRepository<CertificadoMedico>, CertificadoMedicoRepositoryCustom {

    List<CertificadoMedico> findByUser_IdAndStatus(Long userId, int status);

    List<CertificadoMedico> findByUser_IdInAndStatus(java.util.Collection<Long> userIds, int status);

    @Query("""
            SELECT c FROM CertificadoMedico c
            WHERE c.estado = :estado
              AND c.fechaExpiracion <= :fecha
              AND c.status = 0
            """)
    List<CertificadoMedico> findByEstadoAndFechaExpiracionLessThanEqual(
            @Param("estado") CertificadoMedicoEstadoEnum estado,
            @Param("fecha") LocalDate fecha,
            Pageable pageable);
}
