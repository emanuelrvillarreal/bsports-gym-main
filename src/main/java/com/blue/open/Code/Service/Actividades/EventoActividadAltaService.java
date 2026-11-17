package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.EventoActividadAltaCommand;
import com.blue.open.Code.Entity.DTO.EventoActividadDetalleDTO;
import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.EventoActividad;
import com.blue.open.Code.Entity.Enums.EventoActividadEstadoEnum;
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
public class EventoActividadAltaService {

    @Autowired
    private AuthService authService;

    @Autowired
    private EventoActividadAutorizacionService autorizacionService;

    @Autowired
    private EventoActividadRepository eventoActividadRepository;

    @Autowired
    private EventoActividadMapper eventoActividadMapper;

    @Transactional
    public EventoActividadDetalleDTO crear(EventoActividadAltaCommand command, boolean portalProfesor) {
        authService.valid(List.of(PermissionEnum.ACTIVITY_CREATE));
        Company company = authService.getCurrentCompanyOrThrow();
        Long companyId = company.getId();

        validarRangoFechas(command.getInicio(), command.getFin());

        Actividad actividad = autorizacionService.requireActividadActivaParaAlta(command.getActividadUuid(), companyId);
        autorizacionService.requireAlcanceStaffOProfesorAsignado(actividad, companyId, portalProfesor);

        EventoActividad evento = new EventoActividad();
        evento.setCompany(company);
        evento.setActividad(actividad);
        evento.setTitulo(command.getTitulo().trim());
        evento.setDescripcion(command.getDescripcion().trim());
        evento.setUbicacion(command.getUbicacion().trim());
        evento.setInicio(command.getInicio());
        evento.setFin(command.getFin());
        evento.setEstado(EventoActividadEstadoEnum.CONFIRMADO);

        evento = eventoActividadRepository.save(evento);
        return eventoActividadMapper.toDetalleDto(evento);
    }

    private void validarRangoFechas(java.time.LocalDateTime inicio, java.time.LocalDateTime fin) {
        if (inicio == null || fin == null || !fin.isAfter(inicio)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Rango de fechas inválido",
                    "EventoActividadAltaService | fin debe ser posterior a inicio");
        }
    }
}
