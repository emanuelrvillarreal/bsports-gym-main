package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Entity.DTO.ActividadInscripcionCommand;
import com.blue.open.Code.Entity.DTO.CuotaActividadAltaResultDTO;
import com.blue.open.Code.Entity.DTO.InscripcionActividadDTO;
import com.blue.open.Code.Entity.DTO.InscripcionActividadDescuentoCommand;
import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.ActividadHorario;
import com.blue.open.Code.Entity.Db.Categoria;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Descuento;
import com.blue.open.Code.Entity.Db.InscripcionActividad;
import com.blue.open.Code.Entity.Db.InscripcionActividadDescuento;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.MemberEstadoEnum;
import com.blue.open.Code.Repository.ActividadRepository;
import com.blue.open.Code.Repository.DescuentoRepository;
import com.blue.open.Code.Repository.InscripcionActividadDescuentoRepository;
import com.blue.open.Code.Repository.InscripcionActividadRepository;
import com.blue.open.Code.Service.MemberAuthService;
import com.blue.open.Code.Service.Tesoreria.CuotaActividadGeneracionService;
import com.blue.open.Code.Util.CategoriaEdadUtil;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class ActividadInscripcionService {

    private static final int STATUS_ACTIVE = 0;

    @Autowired
    private AuthService authService;

    @Autowired
    private ActividadRepository actividadRepository;

    @Autowired
    private InscripcionActividadRepository inscripcionActividadRepository;

    @Autowired
    private DescuentoRepository descuentoRepository;

    @Autowired
    private InscripcionActividadDescuentoRepository inscripcionActividadDescuentoRepository;

    @Autowired
    private MemberAuthService memberAuthService;

    @Autowired
    private CuotaActividadGeneracionService cuotaActividadGeneracionService;

    @Autowired
    private MemberCategoriaInscripcionService memberCategoriaInscripcionService;

    @Autowired
    private ActividadHorariosService actividadHorariosService;

    @Autowired
    private com.blue.open.Code.Service.CertificadoMedicoVigenciaService certificadoMedicoVigenciaService;

    @Transactional
    public InscripcionActividadDTO inscribir(String actividadUuid, ActividadInscripcionCommand command) {
        Company company = authService.getCurrentCompanyOrThrow();

        Actividad actividad = actividadRepository.findByUuidAndCompany_IdAndStatusForUpdate(actividadUuid, company.getId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Actividad no encontrada",
                        "ActividadInscripcionService | actividadUuid: " + actividadUuid));

        Member member = memberAuthService.resolveMemberByUuid(command.getMemberUuid(), company.getId());

        validarMemberActivo(member);
        certificadoMedicoVigenciaService.exigirSiCorresponde(actividad, member, LocalDate.now());
        Categoria categoriaAsignada = resolverCategoriaPorEdad(actividad, member);
        validarNoDuplicado(actividad, member);
        Set<ActividadHorario> horariosElegidos =
                actividadHorariosService.resolverEleccion(actividad, command.getHorarioUuids(), null);

        InscripcionActividad inscripcion = new InscripcionActividad();
        inscripcion.setCompany(company);
        inscripcion.setActividad(actividad);
        inscripcion.setMember(member);
        inscripcion.getHorarios().addAll(horariosElegidos);
        inscripcion.setFechaInscripcion(LocalDate.now());
        // El alta ya genera la cuota de la fecha de inscripción, así que la próxima arranca un ciclo después.
        boolean pasaAlMesSiguiente = actividad.inscripcionPasaAlMesSiguiente(inscripcion.getFechaInscripcion());
        if (pasaAlMesSiguiente) {
            // Inscripcion despues del dia de corte: no se cobra el mes en curso; el job genera la del mes siguiente.
            inscripcion.setProximaGeneracion(inscripcion.getFechaInscripcion().withDayOfMonth(1).plusMonths(1));
        } else {
            inscripcion.setProximaGeneracion(inscripcion.getFechaInscripcion());
            inscripcion.avanzarProximaGeneracion();
        }

        inscripcion =inscripcionActividadRepository.save(inscripcion);

        memberCategoriaInscripcionService.asignar(member, categoriaAsignada);

        actividadHorariosService.sincronizarCompleta(actividad);

        List<InscripcionActividadDescuentoCommand> descuentoCommands = command.getDescuentos();
        if (descuentoCommands != null) {
            List<InscripcionActividadDescuento> asignaciones = new ArrayList<>();
            for (InscripcionActividadDescuentoCommand descuentoCommand : descuentoCommands) {
                Descuento descuento = descuentoRepository
                        .findByUuidAndCompanyIdAndStatusActive(descuentoCommand.getDescuentoUuid(), company.getId())
                        .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Descuento no encontrado",
                                "ActividadInscripcionService | descuentoUuid: " + descuentoCommand.getDescuentoUuid()));

                asignaciones.add(InscripcionActividadDescuento.asignar(
                        inscripcion, descuento, descuentoCommand.getFechaDesde(), descuentoCommand.getFechaHasta()));
            }
            inscripcionActividadDescuentoRepository.saveAll(asignaciones);
        }

        CuotaActividadAltaResultDTO cuotaResult;
        if (pasaAlMesSiguiente) {
            cuotaResult = new CuotaActividadAltaResultDTO();
            cuotaResult.setPrimeraCuota(inscripcion.getProximaGeneracion());
        } else {
            cuotaResult = cuotaActividadGeneracionService.generarPorInscripcion(
                    member.getId(), actividad.getId(), inscripcion.getId(), inscripcion.getFechaInscripcion());
        }

        return toDto(inscripcion, actividad, member, cuotaResult);
    }

    /**
     * Cambia los horarios de una inscripcion activa sin darla de baja: la cuota y la fecha de
     * inscripcion no cambian.
     *
     * Espera: actividad y socio de la empresa actual y los horarios nuevos (al menos uno).
     * Responde: la inscripcion con sus horarios actualizados.
     * Lanza 404 si el socio no esta inscripto y 400 si algun horario nuevo esta lleno.
     */
    @Transactional
    public InscripcionActividadDTO cambiarHorarios(String actividadUuid, String memberUuid, List<String> horarioUuids) {
        Company company = authService.getCurrentCompanyOrThrow();
        Actividad actividad = actividadRepository.findByUuidAndCompany_IdAndStatusForUpdate(actividadUuid, company.getId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Actividad no encontrada",
                        "ActividadInscripcionService | cambiarHorarios | actividadUuid: " + actividadUuid));
        Member member = memberAuthService.resolveMemberByUuid(memberUuid, company.getId());
        InscripcionActividad inscripcion = inscripcionActividadRepository
                .findByActividad_IdAndMember_IdAndStatus(actividad.getId(), member.getId(), STATUS_ACTIVE)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "El socio no está inscripto en esta actividad",
                        "ActividadInscripcionService | cambiarHorarios | memberId: " + member.getId()));

        Set<ActividadHorario> elegidos = actividadHorariosService.resolverEleccion(actividad, horarioUuids, inscripcion);
        inscripcion.getHorarios().clear();
        inscripcion.getHorarios().addAll(elegidos);
        inscripcionActividadRepository.save(inscripcion);
        actividadHorariosService.sincronizarCompleta(actividad);

        return toDto(inscripcion, actividad, member, null);
    }

    private void validarMemberActivo(Member member) {
        if (member.getEstado() != MemberEstadoEnum.ACTIVO) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "El socio no puede inscribirse en actividades (estado: " + member.getEstado() + ")",
                    "ActividadInscripcionService | memberId: " + member.getId() + " estado: " + member.getEstado());
        }
    }

    /**
     * Devuelve la primera categoría de la actividad (por nombre) cuyo rango de edad admite al socio,
     * o null si la actividad no tiene categorías.
     */
    private Categoria resolverCategoriaPorEdad(Actividad actividad, Member member) {
        Set<Categoria> categorias = actividad.getCategorias();
        if (categorias.isEmpty()) {
            return null;
        }

        User user = member.getUser();
        LocalDate fechaNacimiento = user != null ? user.getFechaNacimiento() : null;
        if (fechaNacimiento == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "El socio no tiene fecha de nacimiento informada",
                    "ActividadInscripcionService | memberId: " + member.getId()
                            + " actividadId: " + actividad.getId());
        }

        int edad = CategoriaEdadUtil.edadMaximaReglamentaria(
                fechaNacimiento, CategoriaEdadUtil.anioReferenciaActual());
        return categorias.stream()
                .sorted(Comparator.comparing(Categoria::getNombre, String.CASE_INSENSITIVE_ORDER))
                .filter(categoria -> categoria.admiteEdad(edad))
                .findFirst()
                .orElseThrow(() -> new CustomException(HttpStatus.BAD_REQUEST,
                        "El socio no cumple el rango de edad de ninguna categoria de la actividad",
                        "ActividadInscripcionService | memberId: " + member.getId()
                                + " actividadId: " + actividad.getId()
                                + " edad: " + edad));
    }

    private void validarNoDuplicado(Actividad actividad, Member member) {
        if (inscripcionActividadRepository.existsByActividad_IdAndMember_IdAndStatus(
                actividad.getId(), member.getId(), STATUS_ACTIVE)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "El socio ya está inscripto en esta actividad",
                    "ActividadInscripcionService | actividadId: " + actividad.getId()
                            + " memberId: " + member.getId());
        }
    }

    InscripcionActividadDTO toDto(
            InscripcionActividad inscripcion,
            Actividad actividad,
            Member member,
            CuotaActividadAltaResultDTO cuotaResult) {
        User user = member.getUser();

        InscripcionActividadDTO dto = new InscripcionActividadDTO();
        dto.setInscripcionUuid(inscripcion.getUuid());
        dto.setActividadUuid(actividad.getUuid());
        dto.setActividadNombre(actividad.getNombre());
        dto.setMemberUuid(member.getUuid());
        dto.setMemberNombre(user != null ? user.getNombre() : null);
        dto.setMemberApellido(user != null ? user.getApellido() : null);
        dto.setFechaInscripcion(inscripcion.getFechaInscripcion());
        dto.setActividadCompleta(actividad.isCompleta());
        dto.setHorarioUuids(inscripcion.getHorarios().stream()
                .filter(horario -> horario.getStatus() == STATUS_ACTIVE)
                .map(ActividadHorario::getUuid)
                .toList());
        dto.setHorarios(actividadHorariosService.obtenerPorActividad(List.of(actividad.getId()))
                .getOrDefault(actividad.getId(), List.of()));
        if (cuotaResult != null) {
            dto.setCuotaGenerada(cuotaResult.isGenerada());
            dto.setCuotaUuid(cuotaResult.getCuotaUuid());
            if (cuotaResult.getPrimeraCuota() != null) {
                dto.setCuotaAdvertencia("Se inscribió después del día de corte: este mes no se cobra. La primera cuota se genera el "
                        + cuotaResult.getPrimeraCuota().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            } else if (cuotaResult.isCubiertaPorPase()) {
                dto.setCuotaAdvertencia("No se cobra la cuota: el socio tiene un pase vigente que incluye esta actividad");
            } else if (cuotaResult.isYaExistia()) {
                dto.setCuotaAdvertencia("Ya tenía la cuota de este período de la actividad; no se generó otra");
            } else if (!cuotaResult.isGenerada()) {
                dto.setCuotaAdvertencia("No se generó la cuota de actividad");
            }
        }
        return dto;
    }
}

