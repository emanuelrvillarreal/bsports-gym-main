package com.blue.open.Code.Service.EventoAcceso;

import com.blue.open.Code.Entity.DTO.EventoAccesoAltaCommand;
import com.blue.open.Code.Entity.DTO.EventoAccesoCobroCommand;
import com.blue.open.Code.Entity.DTO.EventoAccesoCobroResultDTO;
import com.blue.open.Code.Entity.DTO.EventoAccesoDetalleDTO;
import com.blue.open.Code.Entity.DTO.EventoAccesoEdicionCommand;
import com.blue.open.Code.Entity.DTO.EventoAccesoHistorialDetalleDTO;
import com.blue.open.Code.Entity.DTO.EventoAccesoHistorialPaginateRequestDTO;
import com.blue.open.Code.Entity.DTO.EventoAccesoPaginateRequestDTO;
import com.blue.open.Code.Entity.DTO.EventoAccesoScanCommand;
import com.blue.open.Code.Entity.DTO.EventoAccesoScanResultDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.EventoAcceso;
import com.blue.open.Code.Mapper.EventoAccesoMapper;
import com.blue.open.Code.Repository.EventoAccesoRepository;
import com.blue.open.Code.Repository.EventoAccesoSpecifications;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventoAccesoService {

    private static final int MAX_PAGE_SIZE = 20;

    private final AuthService authService;
    private final EventoAccesoRepository eventoAccesoRepository;
    private final EventoAccesoMapper eventoAccesoMapper;
    private final EventoAccesoScanService eventoAccesoScanService;
    private final EventoAccesoHistorialService eventoAccesoHistorialService;
    private final EventoAccesoCobroService eventoAccesoCobroService;

    @Transactional
    public EventoAccesoDetalleDTO crear(EventoAccesoAltaCommand command) {
        Company company = authService.getCurrentCompanyOrThrow();

        EventoAcceso evento = new EventoAcceso();
        evento.setCompany(company);
        evento.setTitulo(command.getTitulo().trim());
        evento.setDescripcion(command.getDescripcion().trim());
        evento.setMonto(command.getMonto());
        evento.setFecha(command.getFecha());
        evento.setPermiteMorosos(command.getPermiteMorosos());
        evento.setPermiteInvitados(command.getPermiteInvitados());
        evento.setMontoMorosos(command.getMontoMorosos());
        evento.setMontoInvitados(command.getMontoInvitados());

        evento = eventoAccesoRepository.save(evento);
        return eventoAccesoMapper.toDetalleDto(evento);
    }

    @Transactional
    public EventoAccesoDetalleDTO editar(String eventoUuid, EventoAccesoEdicionCommand command) {
        Company company = authService.getCurrentCompanyOrThrow();
        EventoAcceso evento = requireEventoActivo(eventoUuid, company.getId());

        evento.setTitulo(command.getTitulo().trim());
        evento.setDescripcion(command.getDescripcion().trim());
        evento.setMonto(command.getMonto());
        evento.setFecha(command.getFecha());
        evento.setPermiteMorosos(command.getPermiteMorosos());
        evento.setPermiteInvitados(command.getPermiteInvitados());
        evento.setMontoMorosos(command.getMontoMorosos());
        evento.setMontoInvitados(command.getMontoInvitados());

        evento = eventoAccesoRepository.save(evento);
        return eventoAccesoMapper.toDetalleDto(evento);
    }

    @Transactional
    public void eliminar(String eventoUuid) {
        Company company = authService.getCurrentCompanyOrThrow();
        EventoAcceso evento = requireEventoActivo(eventoUuid, company.getId());
        evento.setStatus(-1);
        eventoAccesoRepository.save(evento);
    }

    @Transactional(readOnly = true)
    public Page<EventoAccesoDetalleDTO> paginar(EventoAccesoPaginateRequestDTO request) {
        Company company = authService.getCurrentCompanyOrThrow();
        EventoAccesoPaginateRequestDTO req = request != null ? request : new EventoAccesoPaginateRequestDTO();
        int page = Math.max(req.getPage(), 1);
        int size = req.getSize() <= 0 ? 10 : Math.min(req.getSize(), MAX_PAGE_SIZE);

        Specification<EventoAcceso> spec = Specification.where(EventoAccesoSpecifications.forCompany(company.getId()))
                .and(EventoAccesoSpecifications.withPaginateRequest(req));

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(
                Sort.Order.desc("dateCreated"),
                Sort.Order.desc("id")));

        return eventoAccesoRepository.findAll(spec, pageable).map(eventoAccesoMapper::toDetalleDto);
    }

    public EventoAccesoDetalleDTO obtenerPorUuid(String eventoUuid) {
        Company company = authService.getCurrentCompanyOrThrow();
        return eventoAccesoMapper.toDetalleDto(requireEventoActivo(eventoUuid, company.getId()));
    }

    public EventoAccesoScanResultDTO scan(String eventoUuid, EventoAccesoScanCommand command) {
        return eventoAccesoScanService.scan(eventoUuid, command);
    }

    public EventoAccesoCobroResultDTO cobrar(String eventoUuid, EventoAccesoCobroCommand command) {
        return eventoAccesoCobroService.cobrar(eventoUuid, command);
    }

    public Page<EventoAccesoHistorialDetalleDTO> historial(String eventoUuid, EventoAccesoHistorialPaginateRequestDTO request) {
        return eventoAccesoHistorialService.paginar(eventoUuid, request);
    }

    private EventoAcceso requireEventoActivo(String eventoUuid, Long companyId) {
        if (eventoUuid == null || eventoUuid.isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Evento requerido",
                    "EventoAccesoService | eventoUuid vacío");
        }
        return eventoAccesoRepository.findByUuidAndCompanyId(eventoUuid.trim(), companyId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Evento no encontrado",
                        "EventoAccesoService | uuid: " + eventoUuid));
    }
}
