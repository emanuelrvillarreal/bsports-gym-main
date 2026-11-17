package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.EventoActividadDetalleDTO;
import com.blue.open.Code.Entity.DTO.EventoActividadEstadoCommand;
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
public class EventoActividadBajaService {

    @Autowired
    private AuthService authService;

    @Autowired
    private EventoActividadAutorizacionService autorizacionService;

    @Autowired
    private EventoActividadRepository eventoActividadRepository;

    @Autowired
    private EventoActividadMapper eventoActividadMapper;

    @Transactional
    public EventoActividadDetalleDTO cambiarEstado(String eventoUuid, EventoActividadEstadoCommand command, boolean portalProfesor) {
        authService.valid(List.of(PermissionEnum.ACTIVITY_DELETE));
        Long companyId = authService.getCurrentCompanyOrThrow().getId();

        EventoActividad evento = resolverEventoActivo(eventoUuid, companyId);
        autorizacionService.requireAlcanceStaffOProfesorAsignado(evento.getActividad(), companyId, portalProfesor);

        if (evento.getEstado() == command.getEstado()) {
            throw new CustomException(HttpStatus.CONFLICT,
                    "El evento ya se encuentra en estado " + command.getEstado(),
                    "EventoActividadBajaService | uuid: " + eventoUuid);
        }

        evento.setEstado(command.getEstado());
        evento = eventoActividadRepository.save(evento);
        return eventoActividadMapper.toDetalleDto(evento);
    }

    @Transactional
    public void bajaLogica(String eventoUuid, boolean portalProfesor) {
        authService.valid(List.of(PermissionEnum.ACTIVITY_DELETE));
        Long companyId = authService.getCurrentCompanyOrThrow().getId();

        EventoActividad evento = resolverEventoActivo(eventoUuid, companyId);
        autorizacionService.requireAlcanceStaffOProfesorAsignado(evento.getActividad(), companyId, portalProfesor);

        evento.setStatus(-1);
        eventoActividadRepository.save(evento);
    }

    private EventoActividad resolverEventoActivo(String eventoUuid, Long companyId) {
        return eventoActividadRepository.findByUuidAndCompanyIdWithDetails(eventoUuid, companyId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Evento no encontrado",
                        "EventoActividadBajaService | uuid: " + eventoUuid));
    }
}
