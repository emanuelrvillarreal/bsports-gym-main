package com.blue.open.Code.Entity.Db;

import com.blue.open.Core.Entity.Db.AbstractEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@Table(name = "cuotas_sociales_rangos")
@Entity
public class CuotaSocialRango extends AbstractEntity {

    public static final String NOMBRE_DEFAULT = "General";
    public static final int EDAD_HASTA_DEFAULT = 100;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "descripcion", length = 512)
    private String descripcion;

    @Column(name = "edad_hasta", nullable = false)
    private Integer edadHasta;

    @Column(name = "importe", nullable = false, precision = 12, scale = 2)
    private BigDecimal importe;

    /** Sembrado por migración (rango catch-all del club); no puede eliminarse. */
    @Column(name = "es_default", nullable = false)
    private Boolean esDefault = Boolean.FALSE;

    /**
     * Crea el rango catch-all del club (hasta 100 años, importe 0), el mismo que siembra v47.
     * Responde: el rango listo para persistir.
     */
    public static CuotaSocialRango crearDefault(Company company) {
        CuotaSocialRango rango = new CuotaSocialRango();
        rango.company = company;
        rango.nombre = NOMBRE_DEFAULT;
        rango.descripcion = null;
        rango.edadHasta = EDAD_HASTA_DEFAULT;
        rango.importe = BigDecimal.ZERO.setScale(2);
        rango.esDefault = Boolean.TRUE;
        return rango;
    }
}
