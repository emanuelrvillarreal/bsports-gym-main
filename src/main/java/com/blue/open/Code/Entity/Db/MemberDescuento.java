package com.blue.open.Code.Entity.Db;

import com.blue.open.Core.Entity.Db.AbstractEntity;
import com.blue.open.Core.Exceptions.CustomException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

/**
 * Asignación recurrente de un {@link Descuento} a un socio, para la cuota social.
 * <p>
 * La vigencia (desde/hasta) es propia de esta asignación, no del {@link Descuento}: el mismo
 * descuento del catálogo puede tener fechas distintas para cada socio al que se le asigna. No
 * hay restricción de exclusividad: un socio puede tener más de una asignación vigente al mismo
 * tiempo (se aplican todas, ver {@link CuotaDescuento}).
 */
@Getter
@Setter(AccessLevel.NONE)
@Table(name = "member_descuentos")
@Entity
public class MemberDescuento extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "descuento_id", nullable = false)
    private Descuento descuento;

    @Column(name = "fecha_desde", nullable = false)
    private LocalDate fechaDesde;

    @Column(name = "fecha_hasta")
    private LocalDate fechaHasta;

    protected MemberDescuento() {
    }

    public static MemberDescuento asignar(
            Member member, Descuento descuento, LocalDate fechaDesde, LocalDate fechaHasta) {

        if (member == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Socio requerido",
                    "MemberDescuento | asignar | member null");
        }
        if (descuento == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Descuento requerido",
                    "MemberDescuento | asignar | descuento null");
        }
        if (member.getCompany() == null || descuento.getCompany() == null
                || !member.getCompany().getId().equals(descuento.getCompany().getId())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "El descuento no pertenece al gimnasio del socio",
                    "MemberDescuento | asignar | memberId: " + member.getId()
                            + " descuentoId: " + descuento.getId());
        }
        if (fechaDesde == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "La fecha desde es requerida",
                    "MemberDescuento | asignar | fechaDesde null");
        }
        if (fechaHasta != null && fechaHasta.isBefore(fechaDesde)) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "La fecha hasta no puede ser anterior a la fecha desde",
                    "MemberDescuento | asignar | fechaDesde: " + fechaDesde + " fechaHasta: " + fechaHasta);
        }

        MemberDescuento asignacion = new MemberDescuento();
        asignacion.member = member;
        asignacion.descuento = descuento;
        asignacion.fechaDesde = fechaDesde;
        asignacion.fechaHasta = fechaHasta;
        return asignacion;
    }

    public boolean estaVigente(LocalDate fecha) {
        return getStatus() == 0
                && !fecha.isBefore(fechaDesde)
                && (fechaHasta == null || !fecha.isAfter(fechaHasta));
    }

    /** Cierra la vigencia a partir de {@code fecha}, sin perder el historial de la asignación. */
    public void finalizar(LocalDate fecha) {
        if (fecha == null || fecha.isBefore(fechaDesde)) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "La fecha de finalización no puede ser anterior a la fecha desde",
                    "MemberDescuento | finalizar | uuid: " + getUuid() + " fecha: " + fecha);
        }
        this.fechaHasta = fecha;
    }
}
