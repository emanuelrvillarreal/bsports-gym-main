package com.blue.open.Code.Service.EventoAcceso;

import com.blue.open.Code.Entity.DTO.EventoAccesoHistorialDetalleDTO;
import com.blue.open.Code.Entity.DTO.EventoAccesoHistorialPaginateRequestDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.EventoAcceso;
import com.blue.open.Code.Entity.Db.EventoAccesoHistorial;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Enums.EventoAccesoHistorialTipoEnum;
import com.blue.open.Code.Entity.Enums.EventoAccesoScanEstadoEnum;
import com.blue.open.Code.Mapper.EventoAccesoMapper;
import com.blue.open.Code.Repository.EventoAccesoHistorialRepository;
import com.blue.open.Code.Repository.EventoAccesoHistorialSpecifications;
import com.blue.open.Code.Repository.EventoAccesoRepository;
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

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class EventoAccesoHistorialService {

    private static final int MAX_PAGE_SIZE = 20;

    private final AuthService authService;
    private final EventoAccesoRepository eventoAccesoRepository;
    private final EventoAccesoHistorialRepository eventoAccesoHistorialRepository;
    private final EventoAccesoMapper eventoAccesoMapper;

    @Transactional(readOnly = true)
    public Page<EventoAccesoHistorialDetalleDTO> paginar(String eventoUuid, EventoAccesoHistorialPaginateRequestDTO request) {
        Company company = authService.getCurrentCompanyOrThrow();
        EventoAcceso evento = requireEventoActivo(eventoUuid, company.getId());

        EventoAccesoHistorialPaginateRequestDTO req = request != null ? request : new EventoAccesoHistorialPaginateRequestDTO();
        int page = Math.max(req.getPage(), 1);
        int size = req.getSize() <= 0 ? 10 : Math.min(req.getSize(), MAX_PAGE_SIZE);

        Specification<EventoAccesoHistorial> spec = Specification
                .where(EventoAccesoHistorialSpecifications.forEvento(evento.getId()))
                .and(EventoAccesoHistorialSpecifications.withPaginateRequest(req))
                .and(EventoAccesoHistorialSpecifications.withResultado(req.getResultado()));

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(
                Sort.Order.desc("dateCreated"),
                Sort.Order.desc("id")));

        return eventoAccesoHistorialRepository.findAll(spec, pageable).map(eventoAccesoMapper::toHistorialDetalleDto);
    }

    @Transactional
    public void registrarSocio(Company company, EventoAcceso evento, Member member, String nombre,
                                EventoAccesoScanEstadoEnum resultado, String motivo, BigDecimal monto) {
        registrar(company, evento, member, EventoAccesoHistorialTipoEnum.SOCIO, nombre, resultado, motivo, monto);
    }

    @Transactional
    public void registrarInvitado(Company company, EventoAcceso evento, String nombre,
                                   EventoAccesoScanEstadoEnum resultado, String motivo, BigDecimal monto) {
        registrar(company, evento, null, EventoAccesoHistorialTipoEnum.INVITADO, nombre, resultado, motivo, monto);
    }

    private void registrar(Company company, EventoAcceso evento, Member member,
                            EventoAccesoHistorialTipoEnum tipo, String nombre,
                            EventoAccesoScanEstadoEnum resultado, String motivo, BigDecimal monto) {
        EventoAccesoHistorial historial = new EventoAccesoHistorial();
        historial.setCompany(company);
        historial.setEventoAcceso(evento);
        historial.setMember(member);
        historial.setTipo(tipo);
        historial.setNombre(nombre);
        historial.setResultado(resultado);
        historial.setMotivo(motivo);
        historial.setMonto(monto);
        eventoAccesoHistorialRepository.save(historial);
    }

    private EventoAcceso requireEventoActivo(String eventoUuid, Long companyId) {
        if (eventoUuid == null || eventoUuid.isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Evento requerido",
                    "EventoAccesoHistorialService | eventoUuid vacío");
        }
        return eventoAccesoRepository.findByUuidAndCompanyId(eventoUuid.trim(), companyId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Evento no encontrado",
                        "EventoAccesoHistorialService | uuid: " + eventoUuid));
    }
}
