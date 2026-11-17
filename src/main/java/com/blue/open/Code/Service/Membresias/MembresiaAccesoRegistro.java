package com.blue.open.Code.Service.Membresias;

import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.Amenity;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.Membresia;
import com.blue.open.Code.Entity.Db.MembresiaAcceso;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Guarda un ingreso rechazado en su propia transacción: el registro del acceso se aborta con una excepción,
 * pero el intento tiene que quedar para auditoría. Bean aparte para que el REQUIRES_NEW aplique.
 */
@Component
public class MembresiaAccesoRegistro {

    @PersistenceContext
    private EntityManager em;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarRechazo(Long companyId, Long membresiaId, Long memberId, LocalDateTime fecha, String motivo,
                                 Actividad actividad, Amenity amenity) {
        MembresiaAcceso acceso = new MembresiaAcceso();
        acceso.setCompany(em.getReference(Company.class, companyId));
        acceso.setMembresia(membresiaId == null ? null : em.getReference(Membresia.class, membresiaId));
        acceso.setMember(em.getReference(Member.class, memberId));
        acceso.setFecha(fecha);
        acceso.setPermitido(false);
        acceso.setMotivo(motivo.length() > 255 ? motivo.substring(0, 255) : motivo);
        acceso.setActividad(actividad == null ? null : em.getReference(Actividad.class, actividad.getId()));
        acceso.setAmenity(amenity == null ? null : em.getReference(Amenity.class, amenity.getId()));
        em.persist(acceso);
    }
}
