package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.CertificadoMedico;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.CertificadoMedicoEstadoEnum;
import com.blue.open.Code.Entity.Enums.CertificadoSituacionEnum;
import com.blue.open.Code.Repository.CertificadoMedicoRepository;
import com.blue.open.Core.Exceptions.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Decide si un socio tiene certificado médico vigente. Un certificado cuenta solo si está CONFIRMADO por el club y
 * su fecha de expiración no pasó; el cargado por el socio y todavía sin confirmar no habilita nada.
 */
@Service
public class CertificadoMedicoVigenciaService {

    private static final int STATUS_ACTIVE = 0;
    /** Días de anticipación con los que un certificado vigente se informa como "por vencer". */
    public static final int DIAS_POR_VENCER = 30;

    @Autowired
    private CertificadoMedicoRepository certificadoMedicoRepository;

    /** Situación de cada usuario (por id) a {@code hoy}. Los usuarios sin certificados quedan SIN_CERTIFICADO. */
    @Transactional(readOnly = true)
    public Map<Long, CertificadoSituacionEnum> situaciones(Collection<Long> userIds, LocalDate hoy) {
        Map<Long, CertificadoSituacionEnum> resultado = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return resultado;
        }
        Map<Long, List<CertificadoMedico>> porUsuario = new HashMap<>();
        for (CertificadoMedico certificado : certificadoMedicoRepository.findByUser_IdInAndStatus(userIds, STATUS_ACTIVE)) {
            porUsuario.computeIfAbsent(certificado.getUser().getId(), k -> new java.util.ArrayList<>()).add(certificado);
        }
        for (Long userId : userIds) {
            resultado.put(userId, calcular(porUsuario.getOrDefault(userId, List.of()), hoy));
        }
        return resultado;
    }

    public static CertificadoSituacionEnum calcular(List<CertificadoMedico> certificados, LocalDate hoy) {
        LocalDate mayorVigencia = null;
        boolean pendiente = false;
        boolean vencido = false;
        boolean denegado = false;
        for (CertificadoMedico certificado : certificados) {
            CertificadoMedicoEstadoEnum estado = certificado.getEstado();
            if (estado == CertificadoMedicoEstadoEnum.CONFIRMADO && !certificado.getFechaExpiracion().isBefore(hoy)) {
                if (mayorVigencia == null || certificado.getFechaExpiracion().isAfter(mayorVigencia)) {
                    mayorVigencia = certificado.getFechaExpiracion();
                }
            } else if (estado == CertificadoMedicoEstadoEnum.PENDIENTE_VALIDACION) {
                pendiente = true;
            } else if (estado == CertificadoMedicoEstadoEnum.DENEGADO) {
                denegado = true;
            } else {
                vencido = true;
            }
        }
        if (mayorVigencia != null) {
            return mayorVigencia.isAfter(hoy.plusDays(DIAS_POR_VENCER))
                    ? CertificadoSituacionEnum.VIGENTE
                    : CertificadoSituacionEnum.POR_VENCER;
        }
        if (pendiente) {
            return CertificadoSituacionEnum.PENDIENTE_VALIDACION;
        }
        if (vencido) {
            return CertificadoSituacionEnum.VENCIDO;
        }
        return denegado ? CertificadoSituacionEnum.DENEGADO : CertificadoSituacionEnum.SIN_CERTIFICADO;
    }

    public static boolean habilita(CertificadoSituacionEnum situacion) {
        return situacion == CertificadoSituacionEnum.VIGENTE || situacion == CertificadoSituacionEnum.POR_VENCER;
    }

    /** Corta si la actividad exige certificado y el socio no tiene uno vigente. No hace nada si no lo exige. */
    @Transactional(readOnly = true)
    public void exigirSiCorresponde(Actividad actividad, Member member, LocalDate hoy) {
        if (actividad == null || !actividad.isRequiereCertificado()) {
            return;
        }
        User user = member.getUser();
        CertificadoSituacionEnum situacion = user == null
                ? CertificadoSituacionEnum.SIN_CERTIFICADO
                : situaciones(List.of(user.getId()), hoy).get(user.getId());
        if (!habilita(situacion)) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "Esta actividad exige certificado médico vigente y el socio " + descripcion(situacion)
                            + ". Cargá o actualizá el certificado para continuar.",
                    "CertificadoMedicoVigenciaService | actividadId: " + actividad.getId()
                            + " | memberId: " + member.getId() + " | situacion: " + situacion);
        }
    }

    public static String descripcion(CertificadoSituacionEnum situacion) {
        return switch (situacion) {
            case SIN_CERTIFICADO -> "no cargó ninguno";
            case PENDIENTE_VALIDACION -> "tiene uno pendiente de confirmación";
            case VENCIDO -> "tiene el certificado vencido";
            case DENEGADO -> "tiene el certificado rechazado";
            default -> "tiene el certificado vigente";
        };
    }
}
