package com.blue.open.Code.Mapper;

import com.blue.open.Code.Entity.DTO.ActividadDTO;
import com.blue.open.Code.Entity.DTO.ActividadHorarioDTO;
import com.blue.open.Code.Entity.DTO.ActividadInscriptoDTO;
import com.blue.open.Code.Entity.DTO.ActividadPaginateDTO;
import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.ActividadHorario;
import com.blue.open.Code.Entity.Db.Categoria;
import com.blue.open.Code.Entity.Db.InscripcionActividad;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.Profesor;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Service.Actividades.ActividadHorariosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActividadMapper {

    @Autowired
    private ProfesorMapper profesorMapper;

    @Autowired
    private CategoriaMapper categoriaMapper;

    @Autowired
    private ActividadHorariosService actividadHorariosService;

    @Autowired
    private com.blue.open.Code.Service.CertificadoMedicoVigenciaService certificadoMedicoVigenciaService;

    public ActividadDTO modelToDto(Actividad actividad, List<Profesor> profesores) {
        if (actividad == null) {
            return null;
        }
        ActividadDTO dto = new ActividadDTO();
        dto.setActividadUuid(actividad.getUuid());
        dto.setNombre(actividad.getNombre());
        dto.setUbicacion(actividad.getUbicacion());
        dto.setCupoMaximo(actividad.getCupoMaximo());
        dto.setDiasSemana(actividad.getDiasSemana());
        dto.setHoraInicio(actividad.getHoraInicio());
        dto.setHoraFin(actividad.getHoraFin());
        dto.setImporteCuota(actividad.getImporteCuota());
        dto.setPeriodicidadCuota(actividad.getPeriodicidadCuota());
        dto.setDiasVencimientoCuota(actividad.getDiasVencimientoCuota());
        dto.setDiaVencimientoFijo(actividad.getDiaVencimientoFijo());
        dto.setDiaCorteInscripcion(actividad.getDiaCorteInscripcion());
        dto.setCompleta(actividad.isCompleta());
        dto.setRequiereCertificado(actividad.isRequiereCertificado());
        dto.setRecargoHabilitado(actividad.getRecargo().getHabilitado());
        dto.setRecargoTipo(actividad.getRecargo().getTipo());
        dto.setRecargoValor(actividad.getRecargo().getValor());
        dto.setRecargoDiasGracia(actividad.getRecargo().getDiasGracia());
        dto.setCategorias(categoriaMapper.modelsToDtos(List.copyOf(actividad.getCategorias())));
        dto.setProfesores(profesores == null ? List.of() : profesores.stream().map(profesorMapper::modelToDto).toList());
        dto.setHorarios(actividadHorariosService.obtenerPorActividad(List.of(actividad.getId()))
                .getOrDefault(actividad.getId(), List.of()));
        return dto;
    }

    /** Detalle con inscriptos; cada inscripto informa los horarios que eligio. */
    public ActividadDTO modelToDto(
            Actividad actividad,
            List<Profesor> profesores,
            List<InscripcionActividad> inscripciones) {
        ActividadDTO dto = modelToDto(actividad, profesores);
        if (dto == null) {
            return null;
        }
        java.util.Map<Long, com.blue.open.Code.Entity.Enums.CertificadoSituacionEnum> situaciones = java.util.Map.of();
        if (inscripciones != null && actividad.isRequiereCertificado()) {
            situaciones = certificadoMedicoVigenciaService.situaciones(
                    inscripciones.stream()
                            .map(inscripcion -> inscripcion.getMember().getUser())
                            .filter(java.util.Objects::nonNull)
                            .map(User::getId)
                            .toList(),
                    java.time.LocalDate.now());
        }
        final java.util.Map<Long, com.blue.open.Code.Entity.Enums.CertificadoSituacionEnum> situacionesFinal = situaciones;
        dto.setInscriptos(inscripciones == null ? List.of()
                : inscripciones.stream().map(inscripcion -> toInscriptoDto(inscripcion, situacionesFinal)).toList());
        return dto;
    }

    public ActividadPaginateDTO modelToPaginateDto(
            Actividad actividad,
            long cantidadInscriptos,
            List<ActividadHorarioDTO> horarios) {
        if (actividad == null) {
            return null;
        }
        ActividadPaginateDTO dto = new ActividadPaginateDTO();
        dto.setActividadUuid(actividad.getUuid());
        dto.setNombre(actividad.getNombre());
        dto.setUbicacion(actividad.getUbicacion());
        dto.setCupoMaximo(actividad.getCupoMaximo());
        dto.setDiasSemana(actividad.getDiasSemana());
        dto.setHoraInicio(actividad.getHoraInicio());
        dto.setHoraFin(actividad.getHoraFin());
        dto.setImporteCuota(actividad.getImporteCuota());
        dto.setCompleta(actividad.isCompleta());
        dto.setCategoriaNombres(actividad.getCategorias().stream().map(Categoria::getNombre).toList());
        dto.setCantidadInscriptos(cantidadInscriptos);
        dto.setHorarios(horarios == null ? List.of() : horarios);
        return dto;
    }

    private ActividadInscriptoDTO toInscriptoDto(InscripcionActividad inscripcion) {
        return toInscriptoDto(inscripcion, java.util.Map.of());
    }

    private ActividadInscriptoDTO toInscriptoDto(
            InscripcionActividad inscripcion,
            java.util.Map<Long, com.blue.open.Code.Entity.Enums.CertificadoSituacionEnum> situaciones) {
        Member member = inscripcion.getMember();
        ActividadInscriptoDTO dto = new ActividadInscriptoDTO();
        dto.setInscripcionUuid(inscripcion.getUuid());
        dto.setHorarioUuids(inscripcion.getHorarios().stream()
                .filter(horario -> horario.getStatus() == 0)
                .map(ActividadHorario::getUuid)
                .toList());
        if (member.getTitular() != null) {
            dto.setTitularMemberUuid(member.getTitular().getUuid());
        }
        User user = member.getUser();
        if (user != null) {
            dto.setUserUuid(user.getUuid());
            dto.setNombre(user.getNombre());
            dto.setApellido(user.getApellido());
            dto.setEmail(user.getEmail());
            dto.setDni(user.getDni());
            dto.setImageUrl(user.getImageUrl());
            dto.setFechaNacimiento(user.getFechaNacimiento());
            dto.setCertificadoSituacion(situaciones.get(user.getId()));
        }
        return dto;
    }
}
