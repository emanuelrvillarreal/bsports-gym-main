package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.EventoActividadDetalleDTO;
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
public class EventoActividadDetalleQueryService {

    @Autowired
    private AuthService authService;

    @Autowired
    private EventoActividadAutorizacionService autorizacionService;

    @Autowired
    private EventoActividadRepository eventoActividadRepository;

    @Autowired
    private EventoActividadMapper eventoActividadMapper;

    @Transactional(readOnly = true)
    public EventoActividadDetalleDTO obtenerDetalle(String eventoUuid) {
        authService.valid(List.of(
                PermissionEnum.ACTIVITY_VIEW,
                PermissionEnum.PORTAL_ACTIVITIES_VIEW));
        Long companyId = authService.getCurrentCompanyOrThrow().getId();

        EventoActividad evento = eventoActividadRepository.findByUuidAndCompanyIdWithDetails(eventoUuid, companyId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Evento no encontrado",
                        "EventoActividadDetalleQueryService | uuid: " + eventoUuid));

        autorizacionService.requireVisibilidadDetalle(evento, companyId);
        return eventoActividadMapper.toDetalleDto(evento);
    }
}
