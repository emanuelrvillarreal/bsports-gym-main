package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Enums.MemberEstadoEnum;
import com.blue.open.Code.Repository.Custom.MemberRepositoryCustom;
import com.blue.open.Core.Repository.EntityRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends EntityRepository<Member>, MemberRepositoryCustom {

    /**
     * Socios activos del club a los que ya les toca la próxima cuota social
     * ({@code proximaGeneracionCuota <= hasta}). Al generarla, el job avanza el puntero en la
     * misma transacción, así que una segunda corrida no vuelve a encontrar al mismo socio.
     */
    @Query("""
            SELECT m FROM Member m
            LEFT JOIN FETCH m.titular
            WHERE m.company.id = :companyId
              AND m.status = 0
              AND m.estado IN :estados
              AND m.proximaGeneracionCuota <= :hasta
            """)
    List<Member> findConGeneracionCuotaPendiente(
            @Param("companyId") Long companyId,
            @Param("estados") Collection<MemberEstadoEnum> estados,
            @Param("hasta") LocalDate hasta);

    @Query("""
            SELECT m FROM Member m
            LEFT JOIN FETCH m.titular
            JOIN FETCH m.user
            WHERE m.company.id = :companyId
              AND m.status = 0
              AND (m.id = :titularMemberId OR m.titular.id = :titularMemberId)
            """)
    List<Member> findGrupoByTitularId(
            @Param("companyId") Long companyId,
            @Param("titularMemberId") Long titularMemberId);

    /**
     * Igual que {@link #findGrupoByTitularId}, pero además incluye los familiares
     * pendientes de aprobación (status -2), para que el portal pueda mostrarlos
     * junto al resto del grupo mientras esperan que el club acepte la solicitud.
     */
    @Query("""
            SELECT m FROM Member m
            LEFT JOIN FETCH m.titular
            JOIN FETCH m.user
            WHERE m.company.id = :companyId
              AND m.status <> -1
              AND (m.id = :titularMemberId OR m.titular.id = :titularMemberId)
            """)
    List<Member> findGrupoConPendientesByTitularId(
            @Param("companyId") Long companyId,
            @Param("titularMemberId") Long titularMemberId);

    @Query("""
            SELECT m FROM Member m
            LEFT JOIN FETCH m.titular
            LEFT JOIN FETCH m.company
            JOIN FETCH m.user
            WHERE m.id = :memberId
              AND m.status = 0
            """)
    Optional<Member> findByIdWithTitular(@Param("memberId") Long memberId);

    @Query("""
            SELECT m FROM Member m
            LEFT JOIN FETCH m.titular
            JOIN FETCH m.user
            WHERE m.uuid = :uuid
              AND m.company.id = :companyId
              AND m.status = 0
            """)
    Optional<Member> findByUuidAndCompanyIdWithDetails(
            @Param("uuid") String uuid,
            @Param("companyId") Long companyId);

    @Query("""
            SELECT m FROM Member m
            LEFT JOIN FETCH m.titular
            JOIN FETCH m.user
            WHERE m.user.id = :userId
              AND m.company.id = :companyId
              AND m.status = 0
            """)
    Optional<Member> findByUserIdAndCompanyIdWithDetails(
            @Param("userId") Long userId,
            @Param("companyId") Long companyId);

    boolean existsByTitular_IdAndStatus(Long titularId, int status);

    boolean existsByCompany_IdAndUser_IdAndStatus(
            Long companyId,
            Long userId,
            int status);

    boolean existsByCompany_IdAndUser_IdAndStatusIn(
            Long companyId,
            Long userId,
            Collection<Integer> statuses);

    boolean existsByCompany_IdAndNumeroSocioAndStatusAndIdNot(Long companyId, String numeroSocio, int status, Long id);

    boolean existsByCompany_IdAndNumeroSocio(Long companyId, String numeroSocio);

    boolean existsByCompany_IdAndNumeroSocioAndIdNot(Long companyId, String numeroSocio, Long id);

    @Query("""
            SELECT m FROM Member m
            LEFT JOIN FETCH m.titular
            JOIN FETCH m.user
            WHERE m.uuid = :uuid
              AND m.company.id = :companyId
              AND m.status = :status
            """)
    Optional<Member> findByUuidAndCompanyIdAndStatus(
            @Param("uuid") String uuid,
            @Param("companyId") Long companyId,
            @Param("status") int status);

    long countByUser_IdAndStatus(Long userId, int status);

    long countByCompany_IdAndStatus(Long companyId, int status);

    /** Socios del club que no están dados de baja, con su usuario cargado (para el listado de certificados). */
    @Query("""
            SELECT m FROM Member m
            JOIN FETCH m.user u
            WHERE m.company.id = :companyId
              AND m.status = 0
              AND m.estado <> com.blue.open.Code.Entity.Enums.MemberEstadoEnum.INACTIVO
            """)
    List<Member> findVigentesConUsuario(@Param("companyId") Long companyId);

    @Query(value = """
            SELECT MAX(CAST(m.numero_socio AS UNSIGNED))
            FROM members m
            WHERE m.company_id = :companyId
              AND m.numero_socio REGEXP '^[0-9]+$'
            """, nativeQuery = true)
    Long findMaxNumeroSocioByCompanyId(@Param("companyId") Long companyId);

    @Query("""
            SELECT m FROM Member m
            JOIN FETCH m.user u
            WHERE m.company.id = :companyId
              AND m.status = 0
              AND m.titular IS NULL
              AND (u.dni = :identifier OR lower(u.email) = lower(:identifier))
            """)
    Optional<Member> findTitularByUserDniOrEmailAndCompanyId(
            @Param("companyId") Long companyId,
            @Param("identifier") String identifier);

    @Query("""
            SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END
            FROM Member m
            WHERE m.categoria.id = :categoriaId
              AND m.status = 0
            """)
    boolean existsByCategoriaIdAndStatusActive(@Param("categoriaId") Long categoriaId);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE Member m
            SET m.categoria = NULL
            WHERE m.categoria.id = :categoriaId
              AND m.company.id = :companyId
              AND m.status <> -1
            """)
    int clearCategoriaByCategoriaIdAndCompanyId(
            @Param("categoriaId") Long categoriaId,
            @Param("companyId") Long companyId);

    @Query("""
            SELECT m FROM Member m
            JOIN FETCH m.user
            WHERE m.company.id = :companyId
              AND m.status <> -1
            """)
    List<Member> findParaRecalculoCuotaSocialRangoByCompanyIdWithUser(@Param("companyId") Long companyId);

    @Query("""
            SELECT m FROM Member m
            JOIN FETCH m.user
            WHERE m.cuotaSocialRango.id = :cuotaSocialRangoId
              AND m.company.id = :companyId
              AND m.status <> -1
            """)
    List<Member> findParaRecalculoCuotaSocialRangoByCuotaSocialRangoIdAndCompanyIdWithUser(
            @Param("cuotaSocialRangoId") Long cuotaSocialRangoId,
            @Param("companyId") Long companyId);
}
