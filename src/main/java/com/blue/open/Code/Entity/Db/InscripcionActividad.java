package com.blue.open.Code.Entity.Db;

import com.blue.open.Core.Entity.Db.AbstractEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Setter
@Getter
@Table(name = "inscripciones_actividad")
@Entity
public class InscripcionActividad extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actividad_id", nullable = false)
    private Actividad actividad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /** Horarios de la actividad a los que asiste el socio; al menos uno mientras este activa. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "inscripciones_actividad_horarios",
            joinColumns = @JoinColumn(name = "inscripcion_id"),
            inverseJoinColumns = @JoinColumn(name = "horario_id"))
    @BatchSize(size = 50)
    private Set<ActividadHorario> horarios = new LinkedHashSet<>();

    @Column(name = "fecha_inscripcion", nullable = false)
    private LocalDate fechaInscripcion;

    /** Dia en que termino este periodo de inscripcion. NULL mientras esta activa. */
    @Column(name = "fecha_baja")
    private LocalDate fechaBaja;

    @Column(name = "motivo_baja", length = 255)
    private String motivoBaja;

    /** Fecha en la que le toca la próxima cuota de actividad. La avanza {@link #avanzarProximaGeneracion()}. */
    @Column(name = "proxima_generacion", nullable = false)
    private LocalDate proximaGeneracion;

    /** Optimistic locking: evita que dos procesos generen la misma cuota a la vez. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * Avanza un periodo la proxima generacion. Con dia de vencimiento fijo, la generacion se alinea al
     * 1 del mes, asi todos los socios de la actividad reciben la cuota el mismo dia.
     */
    /** Cierra el periodo sin borrar la fila: la reinscripcion crea otra y el historial queda completo. */
    public void cerrarPeriodo(String motivo) {
        this.setStatus(-1);
        this.fechaBaja = LocalDate.now();
        this.motivoBaja = motivo != null && !motivo.isBlank() ? motivo.trim() : null;
    }

    public void avanzarProximaGeneracion() {
        LocalDate base = actividad.tieneDiaVencimientoFijo()
                ? proximaGeneracion.withDayOfMonth(1)
                : proximaGeneracion;
        this.proximaGeneracion = actividad.getPeriodicidadCuota().siguienteFecha(base);
    }
}
