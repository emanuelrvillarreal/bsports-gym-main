package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.EventoActividadDetalleDTO;
import com.blue.open.Code.Entity.DTO.EventoActividadEdicionCommand;
import com.blue.open.Code.Entity.Db.EventoActividad;
import com.blue.open.Code.Mapper.EventoActividadMapper;
import com.blue.open.Code.Repository.EventoActividadRepository;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EventoActividadEdicionService {

    @Autowired
    private AuthService authService;

    @Autowired
    private EventoActividadAutorizacionService autorizacionService;

    @Autowired
    private EventoActividadRepository eventoActividadRepository;

    @Autowired
    private EventoActividadMapper eventoActividadMapper;

    @Transactional
    public EventoActividadDetalleDTO editar(
            String eventoUuid,
            EventoActividadEdicionCommand command,
            boolean portalProfesor) {
        authService.valid(List.of(PermissionEnum.ACTIVITY_EDIT));
        Long companyId = authService.getCurrentCompanyOrThrow().getId();

        validarRangoFechas(command.getInicio(), command.getFin());

        EventoActividad evento = resolverEventoActivo(eventoUuid, companyId);
        autorizacionService.requireAlcanceStaffOProfesorAsignado(evento.getActividad(), companyId, portalProfesor);

        evento.setTitulo(command.getTitulo().trim());
        evento.setDescripcion(command.getDescripcion().trim());
        evento.setUbicacion(command.getUbicacion().trim());
        evento.setInicio(command.getInicio());
        evento.setFin(command.getFin());

        evento = eventoActividadRepository.save(evento);
        return eventoActividadMapper.toDetalleDto(evento);
    }

    private EventoActividad resolverEventoActivo(String eventoUuid, Long companyId) {
        return eventoActividadRepository.findByUuidAndCompanyIdWithDetails(eventoUuid, companyId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Evento no encontrado",
                        "EventoActividadEdicionService | uuid: " + eventoUuid));
    }

    private void validarRangoFechas(java.time.LocalDateTime inicio, java.time.LocalDateTime fin) {
        if (inicio == null || fin == null || !fin.isAfter(inicio)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Rango de fechas inválido",
                    "EventoActividadEdicionService | fin debe ser posterior a inicio");
        }
    }
}
