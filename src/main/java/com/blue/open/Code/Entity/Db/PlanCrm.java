package com.blue.open.Code.Entity.Db;

import com.blue.open.Core.Entity.Db.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Cache local de un plan comercializable publicado por el CRM (blue-crm), sincronizado por
 * CrmCatalogoSyncService a partir de CrmIntegracionService.obtenerPlanes.
 * <p>
 * codigo es el identificador natural que asigna el CRM: es la clave con la que se hace upsert en
 * cada sincronizacion, no cambia entre corridas.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "plan_crm")
public class PlanCrm extends AbstractEntity {

    @Column(name = "codigo", nullable = false, length = 60, unique = true)
    private String codigo;

    @Column(name = "nombre", length = 255)
    private String nombre;

    @Column(name = "descripcion", length = 1000)
    private String descripcion;

    @Column(name = "precio", precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(name = "precio_es_final")
    private Boolean precioEsFinal;

    @Column(name = "alicuota_iva", precision = 5, scale = 2)
    private BigDecimal alicuotaIva;

    // Codigo de periodicidad del CRM (ej: mensual, anual). Puede ser null
    @Column(name = "periodicidad_codigo", length = 60)
    private String periodicidadCodigo;

    // Productos incluidos en el plan (nombre + unidades), serializados como JSON. Es solo una
    // cache de lectura, no hace falta modelarlo como tabla hija para consultarlo
    @Column(name = "productos_json", length = 2000)
    private String productosJson;

    public static PlanCrm crear(String codigo) {
        PlanCrm plan = new PlanCrm();
        plan.codigo = codigo;
        return plan;
    }

    /**
     * Precio del plan con IVA aplicado si corresponde. Replica PlanPrecioService.
     * calcularPrecioEfectivo del CRM (blue-crm): si el precio ya es final, o no hay alicuota
     * informada, se devuelve tal cual el precio crudo que sincroniza el catalogo; si no, se le
     * suma el IVA (precio * alicuotaIva / 100, redondeado a 2 decimales, HALF_UP).
     */
    public BigDecimal precioFinal() {
        if (this.precio == null || Boolean.TRUE.equals(this.precioEsFinal) || this.alicuotaIva == null) {
            return this.precio;
        }
        BigDecimal iva = this.precio.multiply(this.alicuotaIva)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return this.precio.add(iva);
    }

    /** Actualiza los datos del plan con lo ultimo que informo el CRM. */
    public void actualizar(
            String nombre,
            String descripcion,
            BigDecimal precio,
            Boolean precioEsFinal,
            BigDecimal alicuotaIva,
            String periodicidadCodigo,
            String productosJson) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.precioEsFinal = precioEsFinal;
        this.alicuotaIva = alicuotaIva;
        this.periodicidadCodigo = periodicidadCodigo;
        this.productosJson = productosJson;
    }
}
