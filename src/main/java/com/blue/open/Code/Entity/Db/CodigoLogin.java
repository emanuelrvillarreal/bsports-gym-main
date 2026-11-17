package com.blue.open.Code.Entity.Db;

import com.blue.open.Core.Entity.Db.AbstractEntity;
import com.blue.open.Core.Exceptions.CustomException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

/**
 * Código de un solo uso para el login sin contraseña. Solo se persiste el hash del código; el
 * código en claro viaja únicamente en el mail que recibe el usuario.
 * <p>
 * Un usuario tiene a lo sumo un código vigente (status 0): al pedir uno nuevo, el anterior se
 * invalida.
 */
@Getter
@Entity
@Table(name = "codigo_login")
public class CodigoLogin extends AbstractEntity {

    public static final Duration VIGENCIA = Duration.ofMinutes(15);
    public static final Duration COOLDOWN = Duration.ofMinutes(5);

    private static final int STATUS_ACTIVO = 0;
    private static final int STATUS_BAJA = -1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "codigo_hash", length = 64, nullable = false, unique = true, columnDefinition = "CHAR(64)")
    private String codigoHash;

    @Column(name = "fecha_vencimiento", nullable = false)
    private Instant fechaVencimiento;

    @Column(name = "fecha_uso")
    private Instant fechaUso;

    protected CodigoLogin() {
        // único constructor, requerido por JPA
    }

    public static CodigoLogin crearParaUsuario(User user, String codigoHash, Instant ahora) {
        if (user == null) {
            throw new CustomException("No se pudo generar el código de acceso",
                    "CodigoLogin | crearParaUsuario | user nulo");
        }
        if (codigoHash == null || codigoHash.isBlank()) {
            throw new CustomException("No se pudo generar el código de acceso",
                    "CodigoLogin | crearParaUsuario | codigoHash vacío");
        }

        CodigoLogin codigoLogin = new CodigoLogin();
        codigoLogin.user = user;
        codigoLogin.codigoHash = codigoHash;
        codigoLogin.fechaVencimiento = ahora.plus(VIGENCIA);
        return codigoLogin;
    }

    public boolean estaVencido(Instant ahora) {
        return ahora.isAfter(this.fechaVencimiento);
    }

    public boolean fueUsado() {
        return this.fechaUso != null;
    }

    public boolean esUsable(Instant ahora) {
        boolean estaActivo = this.status != null && this.status == STATUS_ACTIVO;
        return estaActivo && !fueUsado() && !estaVencido(ahora);
    }

    public void marcarComoUsado(Instant ahora) {
        if (!esUsable(ahora)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Código inválido o vencido",
                    "CodigoLogin | marcarComoUsado | uuid: " + this.uuid);
        }
        this.fechaUso = ahora;
        invalidar(ahora);
    }

    public void validarFueraDeCooldown(Instant ahora) {
        Instant finCooldown = this.dateCreated.toInstant().plus(COOLDOWN);
        if (ahora.isBefore(finCooldown)) {
            throw new CustomException(HttpStatus.TOO_MANY_REQUESTS,
                    "Ya te enviamos un mail. Por favor, espera 5 minutos para solicitar otro.",
                    "CodigoLogin | validarFueraDeCooldown | uuid: " + this.uuid);
        }
    }

    public void invalidar(Instant ahora) {
        this.status = STATUS_BAJA;
        this.dateDeleted = Timestamp.from(ahora);
    }
}
