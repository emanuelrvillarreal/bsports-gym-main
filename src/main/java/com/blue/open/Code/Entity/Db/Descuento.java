package com.blue.open.Code.Entity.Db;

import com.blue.open.Code.Entity.Enums.DescuentoTipoValorEnum;
import com.blue.open.Core.Entity.Db.AbstractEntity;
import com.blue.open.Core.Exceptions.CustomException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.sql.Types;

/**
 * Catálogo de descuentos de un club, reutilizable entre cuota social y cuota de actividad.
 * <p>
 * No tiene vigencia propia: la vigencia es de cada asignación (ver {@link MemberDescuento} /
 * {@link InscripcionActividadDescuento}), no de la definición del descuento.
 */
@Getter
@Setter(AccessLevel.NONE)
@Table(name = "descuentos")
@Entity
public class Descuento extends AbstractEntity {

    private static final int PORCENTAJE_MIN = 0;
    private static final int PORCENTAJE_MAX = 100;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(name = "titulo", nullable = false)
    private String titulo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "tipo_valor", nullable = false, length = 20)
    private DescuentoTipoValorEnum tipoValor;

    @Column(name = "porcentaje")
    private Integer porcentaje;

    @Column(name = "monto_fijo", precision = 12, scale = 2)
    private BigDecimal montoFijo;

    protected Descuento() {
    }

    public static Descuento dePorcentaje(Company company, String titulo, Integer porcentaje) {
        Descuento descuento = base(company, titulo);
        descuento.aplicarPorcentaje(porcentaje);
        return descuento;
    }

    public static Descuento deMontoFijo(Company company, String titulo, BigDecimal montoFijo) {
        Descuento descuento = base(company, titulo);
        descuento.aplicarMontoFijo(montoFijo);
        return descuento;
    }

    /** Edita un descuento existente para que quede expresado como porcentaje. */
    public void editarComoPorcentaje(String titulo, Integer porcentaje) {
        this.titulo = normalizarTitulo(titulo);
        aplicarPorcentaje(porcentaje);
    }

    /** Edita un descuento existente para que quede expresado como monto fijo. */
    public void editarComoMontoFijo(String titulo, BigDecimal montoFijo) {
        this.titulo = normalizarTitulo(titulo);
        aplicarMontoFijo(montoFijo);
    }

    /** Baja lógica del catálogo. Quien la llame es responsable de limpiar las asignaciones. */
    public void darDeBaja() {
        setStatus(-1);
    }

    /**
     * Importe a descontar en pesos sobre {@code monto}: el porcentaje aplicado, o el monto
     * fijo acotado a {@code monto} para no producir un descuento negativo.
     */
    public BigDecimal resolverValor(BigDecimal monto) {
        if (monto == null) {
            return BigDecimal.ZERO;
        }
        if (tipoValor == DescuentoTipoValorEnum.MONTO_FIJO) {
            return montoFijo.min(monto);
        }
        return monto.multiply(BigDecimal.valueOf(porcentaje))
                .divide(BigDecimal.valueOf(PORCENTAJE_MAX));
    }

    private static Descuento base(Company company, String titulo) {
        if (company == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Company requerida",
                    "Descuento | company null");
        }
        Descuento descuento = new Descuento();
        descuento.company = company;
        descuento.titulo = normalizarTitulo(titulo);
        return descuento;
    }

    private void aplicarPorcentaje(Integer porcentaje) {
        if (porcentaje == null || porcentaje < PORCENTAJE_MIN || porcentaje > PORCENTAJE_MAX) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "El porcentaje debe estar entre 0 y 100",
                    "Descuento | aplicarPorcentaje | porcentaje: " + porcentaje);
        }
        this.tipoValor = DescuentoTipoValorEnum.PORCENTAJE;
        this.porcentaje = porcentaje;
        this.montoFijo = null;
    }

    private void aplicarMontoFijo(BigDecimal montoFijo) {
        if (montoFijo == null || montoFijo.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "El monto fijo debe ser mayor a cero",
                    "Descuento | aplicarMontoFijo | montoFijo: " + montoFijo);
        }
        this.tipoValor = DescuentoTipoValorEnum.MONTO_FIJO;
        this.montoFijo = montoFijo;
        this.porcentaje = null;
    }

    private static String normalizarTitulo(String titulo) {
        if (titulo == null || titulo.isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "El título del descuento es requerido",
                    "Descuento | titulo vacío");
        }
        return titulo.trim();
    }
}
