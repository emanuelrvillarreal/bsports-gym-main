package com.blue.open.Code.Entity.Db;

import com.blue.open.Core.Entity.Db.AbstractEntity;
import com.blue.open.Core.Exceptions.CustomException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

/**
 * Snapshot inmutable de un {@link Descuento} aplicado a una {@link Cuota}, con el título y el
 * importe ya resueltos en el momento de generar la cuota — no queda relación viva con
 * {@link Descuento}, así que un cambio posterior en el catálogo no altera cuotas ya generadas.
 * <p>
 * Una {@code Cuota} puede tener más de una fila: un socio puede tener varios descuentos
 * vigentes al mismo tiempo, y cada uno se calcula de forma independiente sobre el {@code monto}
 * de lista de la cuota (no en cascada uno sobre el saldo que deja el anterior).
 */
@Getter
@Setter(AccessLevel.NONE)
@Table(name = "cuota_descuentos")
@Entity
public class CuotaDescuento extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuota_id", nullable = false)
    private Cuota cuota;

    @Column(name = "titulo", nullable = false)
    private String titulo;

    @Column(name = "valor", nullable = false, precision = 12, scale = 2)
    private BigDecimal valor;

    protected CuotaDescuento() {
    }

    public static CuotaDescuento deDescuento(Cuota cuota, Descuento descuento) {
        if (cuota == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Cuota requerida",
                    "CuotaDescuento | deDescuento | cuota null");
        }
        if (descuento == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Descuento requerido",
                    "CuotaDescuento | deDescuento | descuento null");
        }
        if (cuota.getCompany() == null || descuento.getCompany() == null
                || !cuota.getCompany().getId().equals(descuento.getCompany().getId())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "El descuento no pertenece al gimnasio de la cuota",
                    "CuotaDescuento | deDescuento | descuentoId: " + descuento.getId());
        }

        CuotaDescuento cuotaDescuento = new CuotaDescuento();
        cuotaDescuento.cuota = cuota;
        cuotaDescuento.titulo = descuento.getTitulo();
        cuotaDescuento.valor = descuento.resolverValor(cuota.getMonto());
        return cuotaDescuento;
    }
}
