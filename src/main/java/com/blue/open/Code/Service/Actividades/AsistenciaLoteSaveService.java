package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.AsistenciaLoteCommand;
import com.blue.open.Code.Entity.DTO.AsistenciaLoteItemCommand;
import com.blue.open.Code.Entity.DTO.AsistenciaPlanillaDTO;
import com.blue.open.Code.Entity.Db.AsistenciaActividad;
import com.blue.open.Code.Entity.Db.InscripcionActividad;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.Slot;
import com.blue.open.Code.Repository.AsistenciaActividadRepository;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AsistenciaLoteSaveService {

    @Autowired
    private AuthService authService;

    @Autowired
    private AsistenciaAutorizacionService autorizacionService;

    @Autowired
    private AsistenciaPlanillaQueryService planillaQueryService;

    @Autowired
    private AsistenciaActividadRepository asistenciaActividadRepository;

    @Autowired
    private com.blue.open.Code.Service.CertificadoMedicoVigenciaService certificadoMedicoVigenciaService;

    @Transactional
    public AsistenciaPlanillaDTO guardarLote(String slotUuid, AsistenciaLoteCommand command) {
        authService.valid(List.of(PermissionEnum.ACTIVITY_ATTENDANCE_MANAGE));
        validarPayloadLote(command.getItems());

        Long companyId = authService.getCurrentCompanyOrThrow().getId();
        Slot slot = autorizacionService.requireSlotOperable(slotUuid, companyId, true);
        List<AsistenciaLoteItemCommand> items = command.getItems();

        List<InscripcionActividad> inscripcionesPlanilla = planillaQueryService.cargarInscripcionesPlanilla(slot, companyId);
        Map<String, Member> memberByUuid = indexarMembersPlanilla(inscripcionesPlanilla);
        validarMembersEnPlanilla(items, memberByUuid);

        List<Long> memberIds = new ArrayList<>();
        for (AsistenciaLoteItemCommand item : items) {
            memberIds.add(memberByUuid.get(item.getMemberUuid()).getId());
        }

        validarCertificados(slot.getActividad(), items, memberByUuid);

        Map<Long, AsistenciaActividad> existentesByMemberId = indexarExistentes(slot.getId(), memberIds);
        List<AsistenciaActividad> aPersistir = new ArrayList<>();

        for (AsistenciaLoteItemCommand item : items) {
            Member member = memberByUuid.get(item.getMemberUuid());
            AsistenciaActividad registro = existentesByMemberId.get(member.getId());
            if (registro == null) {
                registro = new AsistenciaActividad();
                registro.setCompany(slot.getCompany());
                registro.setSlot(slot);
                registro.setActividad(slot.getActividad());
                registro.setMember(member);
                registro.setFecha(slot.getInicio().toLocalDate());
            }
            registro.setEstado(item.getEstado());
            aPersistir.add(registro);
        }

        asistenciaActividadRepository.saveAll(aPersistir);
        return planillaQueryService.obtenerPlanilla(slotUuid);
    }

    /** Si la actividad exige certificado, no se puede marcar presente a quien no lo tiene vigente. */
    private void validarCertificados(com.blue.open.Code.Entity.Db.Actividad actividad,
                                     List<AsistenciaLoteItemCommand> items, Map<String, Member> memberByUuid) {
        if (actividad == null || !actividad.isRequiereCertificado()) {
            return;
        }
        List<Member> presentes = new ArrayList<>();
        for (AsistenciaLoteItemCommand item : items) {
            if (item.getEstado() == com.blue.open.Code.Entity.Enums.AsistenciaActividadEstadoEnum.PRESENTE) {
                presentes.add(memberByUuid.get(item.getMemberUuid()));
            }
        }
        List<Long> userIds = presentes.stream().filter(m -> m.getUser() != null).map(m -> m.getUser().getId()).toList();
        Map<Long, com.blue.open.Code.Entity.Enums.CertificadoSituacionEnum> situaciones =
                certificadoMedicoVigenciaService.situaciones(userIds, java.time.LocalDate.now());
        List<String> sinCertificado = new ArrayList<>();
        for (Member member : presentes) {
            com.blue.open.Code.Entity.Enums.CertificadoSituacionEnum situacion = member.getUser() == null
                    ? com.blue.open.Code.Entity.Enums.CertificadoSituacionEnum.SIN_CERTIFICADO
                    : situaciones.get(member.getUser().getId());
            if (!com.blue.open.Code.Service.CertificadoMedicoVigenciaService.habilita(situacion)) {
                sinCertificado.add(member.getUser() != null
                        ? member.getUser().getApellido() + " " + member.getUser().getNombre()
                        : member.getUuid());
            }
        }
        if (!sinCertificado.isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "Esta actividad exige certificado médico vigente. No se puede marcar presente a: "
                            + String.join(", ", sinCertificado),
                    "AsistenciaLoteSaveService | sin certificado: " + sinCertificado);
        }
    }

    private void validarPayloadLote(List<AsistenciaLoteItemCommand> items) {
        if (items == null || items.isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Lote vacío",
                    "AsistenciaLoteSaveService | sin ítems");
        }

        Set<String> memberUuids = new HashSet<>();
        for (AsistenciaLoteItemCommand item : items) {
            if (item.getMemberUuid() == null || item.getMemberUuid().isBlank()) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "Member requerido en cada ítem",
                        "AsistenciaLoteSaveService | memberUuid vacío");
            }
            if (item.getEstado() == null) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "Estado requerido en cada ítem",
                        "AsistenciaLoteSaveService | estado null memberUuid: " + item.getMemberUuid());
            }
            if (!memberUuids.add(item.getMemberUuid())) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "Member repetido en el lote",
                        "AsistenciaLoteSaveService | memberUuid duplicado: " + item.getMemberUuid());
            }
        }
    }

    private Map<String, Member> indexarMembersPlanilla(List<InscripcionActividad> inscripciones) {
        Map<String, Member> memberByUuid = new HashMap<>();
        for (InscripcionActividad inscripcion : inscripciones) {
            Member member = inscripcion.getMember();
            memberByUuid.put(member.getUuid(), member);
        }
        return memberByUuid;
    }

    private void validarMembersEnPlanilla(List<AsistenciaLoteItemCommand> items, Map<String, Member> memberByUuid) {
        for (AsistenciaLoteItemCommand item : items) {
            if (!memberByUuid.containsKey(item.getMemberUuid())) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "Member fuera de la planilla válida",
                        "AsistenciaLoteSaveService | memberUuid: " + item.getMemberUuid());
            }
        }
    }

    private Map<Long, AsistenciaActividad> indexarExistentes(Long slotId, List<Long> memberIds) {
        List<AsistenciaActividad> existentes = asistenciaActividadRepository.findBySlotIdAndMemberIdIn(slotId, memberIds);
        Map<Long, AsistenciaActividad> existentesByMemberId = new HashMap<>();
        for (AsistenciaActividad registro : existentes) {
            existentesByMemberId.put(registro.getMember().getId(), registro);
        }
        return existentesByMemberId;
    }
}
