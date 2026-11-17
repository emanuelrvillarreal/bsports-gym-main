package com.blue.open.Code.Service.EventoAcceso;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.EventoAccesoCobroCommand;
import com.blue.open.Code.Entity.DTO.EventoAccesoCobroResultDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.EventoAcceso;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.EventoAccesoHistorialTipoEnum;
import com.blue.open.Code.Repository.EventoAccesoRepository;
import com.blue.open.Code.Service.EventoAcceso.Cobro.EventoAccesoCobroStrategy;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventoAccesoCobroService {

    private final AuthService authService;
    private final EventoAccesoRepository eventoAccesoRepository;
    private final List<EventoAccesoCobroStrategy> strategies;

    private Map<EventoAccesoHistorialTipoEnum, EventoAccesoCobroStrategy> strategyByTipo;

    @PostConstruct
    void init() {
        strategyByTipo = strategies.stream()
                .collect(Collectors.toMap(EventoAccesoCobroStrategy::getTipo, Function.identity()));
    }

    @Transactional
    public EventoAccesoCobroResultDTO cobrar(String eventoUuid, EventoAccesoCobroCommand command) {
        User registradoPor = authService.valid(List.of(PermissionEnum.TREASURY_COLLECT));
        Company company = authService.getCurrentCompanyOrThrow();
        EventoAcceso evento = requireEventoActivo(eventoUuid, company.getId());

        EventoAccesoCobroStrategy strategy = strategyByTipo.get(command.getTipo());
        if (strategy == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Tipo de cobro no soportado",
                    "EventoAccesoCobroService | tipo: " + command.getTipo());
        }
        return strategy.cobrar(company, registradoPor, evento, command.getPayload());
    }

    private EventoAcceso requireEventoActivo(String eventoUuid, Long companyId) {
        if (eventoUuid == null || eventoUuid.isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Evento requerido",
                    "EventoAccesoCobroService | eventoUuid vacío");
        }
        return eventoAccesoRepository.findByUuidAndCompanyId(eventoUuid.trim(), companyId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Evento no encontrado",
                        "EventoAccesoCobroService | uuid: " + eventoUuid));
    }
}
