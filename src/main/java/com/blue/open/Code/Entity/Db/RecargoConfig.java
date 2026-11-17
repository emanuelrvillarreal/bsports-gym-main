package com.blue.open.Code.Entity.Db;

import com.blue.open.Code.Entity.Enums.RecargoTipoEnum;
import com.blue.open.Core.Exceptions.CustomException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Types;
import java.time.LocalDate;

/**
 * Política de recargo por pago fuera de término. La usan la cuota social (en Company) y cada actividad:
 * cada una guarda la suya, con las mismas columnas recargo_* en su tabla.
 */
@Embeddable
@Getter
public class RecargoConfig {

    @Column(name = "recargo_habilitado", nullable = false)
    private Boolean habilitado = false;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "recargo_tipo", length = 20)
    private RecargoTipoEnum tipo;

    /** Importe fijo o porcentaje (0-100), según {@link #tipo}. */
    @Column(name = "recargo_valor", precision = 12, scale = 2)
    private BigDecimal valor;

    @Column(name = "recargo_dias_gracia", nullable = false)
    private Integer diasGracia = 0;

    /** Día en que se activó: el recargo solo alcanza a cuotas que vencen desde entonces, nunca a deuda anterior. */
    @Column(name = "recargo_habilitado_desde")
    private LocalDate habilitadoDesde;

    public boolean estaActivo() {
        return Boolean.TRUE.equals(habilitado) && tipo != null && valor != null && valor.signum() > 0;
    }

    public int diasGraciaEfectivo() {
        return diasGracia != null && diasGracia > 0 ? diasGracia : 0;
    }

    /**
     * Configura el recargo. Al activarlo (estaba apagado) registra la fecha de activación. Los parámetros null
     * conservan el valor actual. Lanza CustomException si el resultado de activarlo es inválido.
     */
    public void configurar(Boolean nuevoHabilitado, RecargoTipoEnum nuevoTipo, BigDecimal nuevoValor,
                           Integer nuevosDiasGracia, LocalDate hoy) {
        if (nuevosDiasGracia != null) {
            if (nuevosDiasGracia < 0) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "Días de gracia inválidos",
                        "Los días de gracia del recargo no pueden ser negativos.");
            }
            this.diasGracia = nuevosDiasGracia;
        }
        if (nuevoTipo != null) {
            this.tipo = nuevoTipo;
        }
        if (nuevoValor != null) {
            this.valor = nuevoValor.setScale(2, RoundingMode.HALF_UP);
        }
        if (nuevoHabilitado == null) {
            return;
        }
        if (nuevoHabilitado) {
            if (tipo == null || valor == null || valor.signum() <= 0) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "Recargo incompleto",
                        "Para activar el recargo indique el tipo (fijo o porcentaje) y un valor mayor a cero.");
            }
            if (tipo == RecargoTipoEnum.PORCENTAJE && valor.compareTo(new BigDecimal("100")) > 0) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "Porcentaje de recargo inválido",
                        "El porcentaje de recargo no puede superar 100.");
            }
            if (!Boolean.TRUE.equals(habilitado)) {
                this.habilitadoDesde = hoy;
            }
        }
        this.habilitado = nuevoHabilitado;
    }

    /** Recargo a cobrar sobre una cuota cuyo neto (monto menos descuentos) es {@code base}. */
    public BigDecimal calcular(BigDecimal base) {
        if (!estaActivo() || base == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal importe = tipo == RecargoTipoEnum.PORCENTAJE
                ? base.multiply(valor).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
                : valor;
        return importe.setScale(2, RoundingMode.HALF_UP);
    }
}
