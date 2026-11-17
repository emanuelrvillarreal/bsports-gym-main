package com.blue.open.Code.Entity.Db;

import com.blue.open.Core.Entity.Db.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Estado de bloqueo de acceso a la plataforma de un club, por facturacion vencida (1:1 con
 * Company). Lo consulta AccesoBloqueadoInterceptor en cada request y lo actualiza
 * CompanyAccesoService.
 * <p>
 * Nace siempre desbloqueada. Una company sin fila en esta tabla se trata como no bloqueada (ver
 * CompanyAccesoService), asi que no hace falta backfill para companies existentes.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "company_bloqueo_acceso")
public class CompanyBloqueoAcceso extends AbstractEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false, unique = true)
    private Company company;

    @Column(name = "bloqueado", nullable = false, columnDefinition = "BIT")
    private Boolean bloqueado = false;

    public static CompanyBloqueoAcceso crearDesbloqueada(Company company) {
        CompanyBloqueoAcceso estado = new CompanyBloqueoAcceso();
        estado.company = company;
        estado.bloqueado = false;
        return estado;
    }

    /** Corta el acceso del club a la plataforma. */
    public void bloquear() {
        this.bloqueado = true;
    }

    /** Restablece el acceso del club a la plataforma. */
    public void desbloquear() {
        this.bloqueado = false;
    }

    public boolean estaBloqueada() {
        return Boolean.TRUE.equals(this.bloqueado);
    }
}
