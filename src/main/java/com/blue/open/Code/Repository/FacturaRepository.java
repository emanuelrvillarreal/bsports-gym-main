package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Factura;
import com.blue.open.Code.Entity.Enums.FacturaEstadoEnum;
import com.blue.open.Code.Repository.Custom.FacturaRepositoryCustom;
import com.blue.open.Core.Repository.EntityRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Acceso a las facturas del club.
 * Lo usa FacturaService para registrar las deudas que informa el CRM (blue-crm) y para
 * consultar las facturas de un club por estado.
 */
public interface FacturaRepository extends EntityRepository<Factura>, FacturaRepositoryCustom {

    /**
     * Busca la factura correspondiente a una factura del CRM.
     * <p>
     * No filtra por status: una factura dada de baja igual tiene que frenar el alta duplicada,
     * si no se perderia la idempotencia frente a los reintentos del webhook.
     */
    Optional<Factura> findFirstByUuidFacturaCrm(String uuidFacturaCrm);

    /**
     * Facturas activas que todavia no tienen club y son del cliente indicado, por uuidCliente o
     * por CUIT: las que llegaron del CRM antes de que el club se diera de alta. El CUIT se espera
     * normalizado (solo digitos). Cualquiera de los dos puede venir null.
     */
    @Query("SELECT f FROM Factura f WHERE f.company IS NULL AND f.status = :status "
            + "AND (f.uuidCliente = :uuidCliente OR f.cuit = :cuit)")
    List<Factura> buscarSinClubPorClienteOCuit(
            @Param("uuidCliente") String uuidCliente, @Param("cuit") String cuit, @Param("status") int status);

    /** Devuelve las facturas activas de un club en un estado determinado. */
    List<Factura> findByCompanyAndEstadoAndStatus(Company company, FacturaEstadoEnum estado, int status);

    /** Indica si el club todavia tiene alguna factura activa en el estado indicado. */
    boolean existsByCompanyAndEstadoAndStatus(Company company, FacturaEstadoEnum estado, int status);

    /** Resuelve facturas por su uuid local, acotadas al club: nadie puede pagar facturas ajenas. */
    List<Factura> findByUuidInAndCompanyAndStatus(List<String> uuids, Company company, int status);


    /** Indica si el club tiene historial de facturacion CRM (al menos una factura activa). */
    boolean existsByCompanyAndStatus(Company company, int status);

    /** Facturas activas en un estado, vencidas antes de la fecha indicada. Paginado para lotes. */
    List<Factura> findByEstadoAndFechaVencimientoLessThanAndStatus(
            FacturaEstadoEnum estado, LocalDate fecha, int status, Pageable pageable);

    /**
     * Id de la ultima factura (mayor id) de cada club entre los indicados, agrupando por
     * company.id — el mayor id coincide en la practica con la mayor fechaGeneracion, porque las
     * facturas se crean en el mismo orden en que llegan del CRM. Se usa junto con
     * {@code findAllById} para traer las entidades completas en una segunda query, y asi evitar
     * el N+1 de pedir la ultima factura club por club en un paginado.
     */
    @Query("SELECT MAX(f.id) FROM Factura f WHERE f.company.id IN :companyIds AND f.status = :status "
            + "GROUP BY f.company.id")
    List<Long> findUltimaFacturaIdPorCompanyIds(
            @Param("companyIds") List<Long> companyIds, @Param("status") int status);

    /** Trae las facturas completas por id, con company ya inicializada (JOIN FETCH). */
    @Query("SELECT f FROM Factura f JOIN FETCH f.company WHERE f.id IN :ids")
    List<Factura> findByIdInWithCompany(@Param("ids") List<Long> ids);
}
