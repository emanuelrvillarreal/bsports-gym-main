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
 * Cache local de un producto comercializable publicado por el CRM (blue-crm), sincronizado por
 * CrmCatalogoSyncService a partir de CrmIntegracionService.obtenerProductos.
 * <p>
 * codigo es el identificador natural que asigna el CRM: es la clave con la que se hace upsert en
 * cada sincronizacion, no cambia entre corridas.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "productos_crm")
public class ProductoCrm extends AbstractEntity {

    @Column(name = "codigo", nullable = false, length = 60, unique = true)
    private String codigo;

    @Column(name = "nombre", length = 255)
    private String nombre;

    @Column(name = "descripcion", length = 1000)
    private String descripcion;

    @Column(name = "precio", precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(name = "alicuota_iva", precision = 5, scale = 2)
    private BigDecimal alicuotaIva;

    // Unidad de medida del producto (ej: unidad, hora). Puede ser null
    @Column(name = "unidad", length = 60)
    private String unidad;

    public static ProductoCrm crear(String codigo) {
        ProductoCrm producto = new ProductoCrm();
        producto.codigo = codigo;
        return producto;
    }

    /**
     * Precio del producto con IVA aplicado si corresponde. A diferencia de PlanCrm, no hay
     * precioEsFinal: el precio de un producto en el CRM siempre es neto, asi que el IVA se suma
     * siempre que haya alicuota informada.
     */
    public BigDecimal precioFinal() {
        if (this.precio == null || this.alicuotaIva == null) {
            return this.precio;
        }
        BigDecimal iva = this.precio.multiply(this.alicuotaIva)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return this.precio.add(iva);
    }

    /** Actualiza los datos del producto con lo ultimo que informo el CRM. */
    public void actualizar(String nombre, String descripcion, BigDecimal precio, BigDecimal alicuotaIva, String unidad) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.alicuotaIva = alicuotaIva;
        this.unidad = unidad;
    }
}
