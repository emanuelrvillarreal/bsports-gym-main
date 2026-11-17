package com.blue.open.Code.Service;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.Db.Member;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import com.blue.open.Code.Repository.MemberRepository;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Número de socio por club. La base tiene una clave única (company_id, numero_socio), así que
 * nunca puede haber dos iguales en el mismo club, sin importar el estado del socio (un número de
 * un socio dado de baja sigue ocupado). Este servicio valida antes de guardar para devolver un
 * 409 con un mensaje claro en lugar de un error de base.
 */
@Service
public class MemberNumeroSocioService {

    public static final String MENSAJE_EN_USO = "El número de socio %s ya está en uso";

    private static final String CLAVE_UNICA = "uk_members_numero_socio";

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AuthService authService;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Guarda y fuerza el INSERT/UPDATE en el momento: si otro usuario guardó el mismo número entre la
     * validación y el guardado, la clave única falla acá y se responde 409 en lugar de un 500.
     */
    public Member guardar(Member member, Long companyId) {
        Member guardado = memberRepository.save(member);
        try {
            entityManager.flush();
        } catch (DataIntegrityViolationException ex) {
            throw traducir(ex, member.getNumeroSocio(), companyId);
        } catch (jakarta.persistence.PersistenceException ex) {
            throw traducir(new DataIntegrityViolationException(String.valueOf(ex.getMessage()), ex),
                    member.getNumeroSocio(), companyId);
        }
        return guardado;
    }

    /** Para mostrar en el alta el número que se va a asignar si no se elige otro. */
    public String siguienteDelClubActual() {
        authService.valid(List.of(PermissionEnum.MEMBER_CREATE, PermissionEnum.MEMBER_EDIT));
        return generateNext(authService.getCurrentCompanyOrThrow().getId());
    }

    /** Siguiente número libre: el mayor número numérico del club + 1. */
    public String generateNext(Long companyId) {
        Long max = memberRepository.findMaxNumeroSocioByCompanyId(companyId);
        return String.valueOf((max == null ? 0L : max) + 1);
    }

    /**
     * Devuelve el número pedido si está libre, o el siguiente si no se pidió ninguno.
     *
     * @param excluirMemberId socio que se está editando (puede conservar su propio número); null en altas.
     */
    public String resolver(Long companyId, Integer pedido, Long excluirMemberId) {
        if (pedido == null) {
            return generateNext(companyId);
        }
        if (pedido <= 0) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "El número de socio debe ser mayor a 0",
                    "MemberNumeroSocioService | numeroSocio: " + pedido);
        }
        String numero = String.valueOf(pedido);
        boolean enUso = excluirMemberId == null
                ? memberRepository.existsByCompany_IdAndNumeroSocio(companyId, numero)
                : memberRepository.existsByCompany_IdAndNumeroSocioAndIdNot(companyId, numero, excluirMemberId);
        if (enUso) {
            throw enUso(numero, companyId);
        }
        return numero;
    }

    /**
     * Traduce la violación de la clave única del número (otro usuario guardó el mismo número entre la
     * validación y el guardado) a un 409. Cualquier otra violación se relanza sin cambios.
     */
    public RuntimeException traducir(DataIntegrityViolationException ex, String numero, Long companyId) {
        Throwable causa = ex.getMostSpecificCause();
        String mensaje = causa != null && causa.getMessage() != null ? causa.getMessage() : "";
        return mensaje.toLowerCase().contains(CLAVE_UNICA) ? enUso(numero, companyId) : ex;
    }

    public CustomException enUso(String numero, Long companyId) {
        return new CustomException(HttpStatus.CONFLICT, String.format(MENSAJE_EN_USO, numero),
                "MemberNumeroSocioService | numeroSocio: " + numero + " companyId: " + companyId);
    }
}
