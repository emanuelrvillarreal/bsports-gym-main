package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.ClubPresencia;
import com.blue.open.Code.Repository.Custom.ClubPresenciaRepositoryCustom;
import com.blue.open.Core.Repository.EntityRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ClubPresenciaRepository extends EntityRepository<ClubPresencia>, ClubPresenciaRepositoryCustom {

    @Query("""
            SELECT cp FROM ClubPresencia cp
            JOIN FETCH cp.user
            WHERE cp.company.id = :companyId
              AND cp.status = 0
              AND cp.presente = :presente
            ORDER BY cp.ultimoAccesoAt DESC
            """)
    List<ClubPresencia> findByCompanyIdAndPresenteWithUser(
            @Param("companyId") Long companyId,
            @Param("presente") boolean presente);

    @Query("""
            SELECT cp FROM ClubPresencia cp
            JOIN FETCH cp.user
            WHERE cp.company.id = :companyId
              AND cp.user.id = :userId
              AND cp.status = 0
            """)
    Optional<ClubPresencia> findByCompanyIdAndUserIdWithUser(
            @Param("companyId") Long companyId,
            @Param("userId") Long userId);

    @Query("""
            SELECT cp FROM ClubPresencia cp
            WHERE cp.company.id = :companyId
              AND cp.user.id = :userId
            """)
    Optional<ClubPresencia> findByCompanyIdAndUserId(
            @Param("companyId") Long companyId,
            @Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("""
            UPDATE ClubPresencia cp
            SET cp.presente = :presente,
                cp.ultimoAccesoAt = :ultimoAccesoAt,
                cp.status = 0
            WHERE cp.company.id = :companyId
              AND cp.user.id = :userId
            """)
    int updatePresenciaByCompanyIdAndUserId(
            @Param("companyId") Long companyId,
            @Param("userId") Long userId,
            @Param("presente") boolean presente,
            @Param("ultimoAccesoAt") LocalDateTime ultimoAccesoAt);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("""
            INSERT INTO ClubPresencia (uuid, status, company, user, presente, ultimoAccesoAt)
            SELECT :uuid, 0, c, u, :presente, :ultimoAccesoAt
            FROM Company c, User u
            WHERE c.id = :companyId
              AND u.id = :userId
            """)
    int insertPresenciaByCompanyIdAndUserId(
            @Param("uuid") String uuid,
            @Param("companyId") Long companyId,
            @Param("userId") Long userId,
            @Param("presente") boolean presente,
            @Param("ultimoAccesoAt") LocalDateTime ultimoAccesoAt);

    @Query("""
            SELECT COUNT(cp) FROM ClubPresencia cp
            WHERE cp.company.id = :companyId
              AND cp.status = 0
              AND cp.presente = :presente
            """)
    long countByCompanyIdAndPresente(
            @Param("companyId") Long companyId,
            @Param("presente") boolean presente);
}
