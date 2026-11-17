package com.blue.open.Code.Entity.Db;

import com.blue.open.Core.Entity.Db.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Suscripcion del club a su plan del CRM (blue-crm), 1:1 con Company. Nace con la respuesta que
 * dejo POST integraciones/cliente/suscribir al dar de alta el club: uuidSuscripcion,
 * uuidFacturaInicial, estado y fechaProximaFacturacion.
 * <p>
 * Lo crea CompanySuscripcionCrmService en tres momentos: al suscribir el club a mano
 * (SysadminSuscripcionService), y en el alta, tanto manual (SysadminClubService.registerClub)
 * como por webhook ALTA_CLIENTE (CompanyService.registrarAltaCliente), cuando el CRM ya le
 * conoce un plan al cliente (ClubSuscripcionInicialService). A partir de ahi lo mantiene
 * espejado SuscripcionVerificacionService, que lo refresca a diario con lo que informa el CRM: de esta
 * tabla salen los cupos de socios y administradores del plan, asi que si quedara congelada en el
 * momento del alta un club que cambia de plan conservaria el cupo viejo para siempre.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "company_suscripcion_crm")
public class CompanySuscripcionCrm extends AbstractEntity {

    /** Estado con el que el CRM marca una suscripcion dada de baja. */
    public static final String ESTADO_ANULADA = "ANULADA";

    /** Estado con el que el CRM marca una suscripcion cuya primera factura todavia no se pago. */
    public static final String ESTADO_PENDIENTE_PAGO = "PENDIENTE_PAGO";

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false, unique = true)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_crm_id", nullable = false)
    private PlanCrm plan;

    @Column(name = "uuid_suscripcion_crm", length = 60)
    private String uuidSuscripcionCrm;

    @Column(name = "uuid_factura_inicial_crm", length = 60)
    private String uuidFacturaInicialCrm;

    // Estado de la suscripcion al momento de suscribir. Se deja como String a proposito: el
    // catalogo de estados del CRM es abierto (BORRADOR, ACTIVA, ANULADA, PENDIENTE_PAGO, ...)
    @Column(name = "estado", length = 30)
    private String estado;

    @Column(name = "fecha_proxima_facturacion")
    private LocalDateTime fechaProximaFacturacion;

    public static CompanySuscripcionCrm crear(
            Company company,
            PlanCrm plan,
            String uuidSuscripcionCrm,
            String uuidFacturaInicialCrm,
            String estado,
            LocalDateTime fechaProximaFacturacion) {
        CompanySuscripcionCrm registro = new CompanySuscripcionCrm();
        registro.company = company;
        registro.actualizar(plan, uuidSuscripcionCrm, uuidFacturaInicialCrm, estado, fechaProximaFacturacion);
        return registro;
    }

    /** Sobrescribe los datos de la suscripcion. Se usa tambien al re-registrar un club reactivado. */
    public void actualizar(
            PlanCrm plan,
            String uuidSuscripcionCrm,
            String uuidFacturaInicialCrm,
            String estado,
            LocalDateTime fechaProximaFacturacion) {
        this.plan = plan;
        this.uuidSuscripcionCrm = uuidSuscripcionCrm;
        this.uuidFacturaInicialCrm = uuidFacturaInicialCrm;
        this.estado = estado;
        this.fechaProximaFacturacion = fechaProximaFacturacion;
    }

    /**
     * Refresca los datos con lo que informa GET integraciones/cliente/planes.
     * <p>
     * No toca uuidFacturaInicialCrm a proposito: es un dato del alta que ese endpoint no
     * devuelve, y sobrescribirlo con null perderia la referencia a la factura inicial.
     */
    public void refrescarDesdeCrm(
            PlanCrm plan,
            String uuidSuscripcionCrm,
            String estado,
            LocalDateTime fechaProximaFacturacion) {
        this.plan = plan;
        this.uuidSuscripcionCrm = uuidSuscripcionCrm;
        this.estado = estado;
        this.fechaProximaFacturacion = fechaProximaFacturacion;
    }

    /**
     * Marca la suscripcion como dada de baja, conservando el plan y las fechas como ultimo dato
     * conocido. No borra la fila: sirve de historial de a que plan estuvo suscripto el club.
     */
    public void anular() {
        this.estado = ESTADO_ANULADA;
        this.fechaProximaFacturacion = null;
    }
}
