package com.blue.open.Code.Service.EventoAcceso;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.EventoAccesoScanCommand;
import com.blue.open.Code.Entity.DTO.EventoAccesoScanResultDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.EventoAcceso;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.EventoAccesoScanEstadoEnum;
import com.blue.open.Code.Entity.Enums.MemberEstadoEnum;
import com.blue.open.Code.Mapper.EventoAccesoMapper;
import com.blue.open.Code.Repository.EventoAccesoRepository;
import com.blue.open.Code.Repository.MemberRepository;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Repository.UserRepository;
import com.blue.open.Core.Service.Auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventoAccesoScanService {

    private static final int STATUS_ACTIVE = 0;

    private final AuthService authService;
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final EventoAccesoRepository eventoAccesoRepository;
    private final EventoAccesoMapper eventoAccesoMapper;
    private final EventoAccesoHistorialService eventoAccesoHistorialService;

    @Transactional
    public EventoAccesoScanResultDTO scan(String eventoUuid, EventoAccesoScanCommand command) {
        authService.valid(List.of(PermissionEnum.TREASURY_COLLECT));
        Company company = authService.getCurrentCompanyOrThrow();
        EventoAcceso evento = requireEventoActivo(eventoUuid, company.getId());

        User user = requireUserInCompany(command.getUserUuid(), company.getId());
        Member member = requireMemberForUser(user, company.getId());

        String nombre = user.getNombre() != null ? user.getNombre() : "";
        String apellido = user.getApellido() != null ? user.getApellido() : "";
        String numeroSocio = member.getNumeroSocio() != null ? member.getNumeroSocio() : "-";

        EventoAccesoScanEstadoEnum estado;
        String motivo = null;
        BigDecimal montoAPagar = null;

        switch (member.getEstado()) {
            case ACTIVO -> {
                BigDecimal monto = evento.getMonto();
                boolean gratuito = monto != null && monto.compareTo(BigDecimal.ZERO) == 0;
                if (gratuito) {
                    estado = EventoAccesoScanEstadoEnum.ACCESO_PERMITIDO;
                } else {
                    estado = EventoAccesoScanEstadoEnum.DEBE_PAGAR;
                    motivo = "%s %s (socio %s) debe pagar para acceder al evento \"%s\""
                            .formatted(nombre, apellido, numeroSocio, evento.getTitulo());
                    montoAPagar = monto;
                }
            }
            case MOROSO -> {
                if (Boolean.TRUE.equals(evento.getPermiteMorosos())) {
                    BigDecimal montoMorosos = evento.getMontoMorosos();
                    boolean gratuito = montoMorosos != null && montoMorosos.compareTo(BigDecimal.ZERO) == 0;
                    if (gratuito) {
                        estado = EventoAccesoScanEstadoEnum.ACCESO_PERMITIDO;
                    } else {
                        estado = EventoAccesoScanEstadoEnum.DEBE_PAGAR;
                        motivo = "%s %s (socio %s) es moroso y debe pagar para acceder al evento \"%s\""
                                .formatted(nombre, apellido, numeroSocio, evento.getTitulo());
                        montoAPagar = montoMorosos;
                    }
                } else {
                    estado = EventoAccesoScanEstadoEnum.BLOQUEADO;
                    motivo = "%s %s (socio %s) es moroso y este evento no permite el acceso de morosos"
                            .formatted(nombre, apellido, numeroSocio);
                }
            }
            case INHABILITADO, SUSPENDIDO, INACTIVO -> {
                estado = EventoAccesoScanEstadoEnum.BLOQUEADO;
                motivo = "Acceso no permitido para socios en estado " + formatEstado(member.getEstado());
            }
            default -> throw new IllegalStateException("Estado de member no soportado: " + member.getEstado());
        }

        EventoAccesoScanResultDTO dto = eventoAccesoMapper.toScanResultDto(member);
        dto.setEstado(estado);
        dto.setMotivo(motivo);
        dto.setMontoAPagar(montoAPagar);

        String nombreSnapshot = (nombre + " " + apellido).trim();
        eventoAccesoHistorialService.registrarSocio(company, evento, member, nombreSnapshot, estado, motivo, montoAPagar);

        return dto;
    }

    private Member requireMemberForUser(User user, Long companyId) {
        return memberRepository.findByUserIdAndCompanyIdWithDetails(user.getId(), companyId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Socio no encontrado",
                        "EventoAccesoScanService | userId: " + user.getId() + ", companyId: " + companyId));
    }

    private User requireUserInCompany(String userUuid, Long companyId) {
        if (userUuid == null || userUuid.isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Usuario requerido",
                    "EventoAccesoScanService | userUuid vacío");
        }

        User user = userRepository.findFirstByUuidWithDetails(userUuid.trim());
        if (user == null || user.getStatus() == null || user.getStatus() != STATUS_ACTIVE) {
            throw new CustomException(HttpStatus.NOT_FOUND, "Usuario no encontrado",
                    "EventoAccesoScanService | userUuid: " + userUuid);
        }

        if (user.getUserCompanyRoles() == null || user.getUserCompanyRoles().stream()
                .noneMatch(ucr -> ucr.getCompany() != null && companyId.equals(ucr.getCompany().getId()))) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "El usuario no pertenece a este gimnasio",
                    "EventoAccesoScanService | userUuid: " + userUuid + ", companyId: " + companyId);
        }

        return user;
    }

    private EventoAcceso requireEventoActivo(String eventoUuid, Long companyId) {
        if (eventoUuid == null || eventoUuid.isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Evento requerido",
                    "EventoAccesoScanService | eventoUuid vacío");
        }
        return eventoAccesoRepository.findByUuidAndCompanyId(eventoUuid.trim(), companyId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Evento no encontrado",
                        "EventoAccesoScanService | uuid: " + eventoUuid));
    }

    private String formatEstado(MemberEstadoEnum estado) {
        return estado.name().toLowerCase().replace('_', ' ');
    }
}
