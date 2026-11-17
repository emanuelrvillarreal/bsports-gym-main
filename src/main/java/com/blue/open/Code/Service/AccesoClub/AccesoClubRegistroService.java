package com.blue.open.Code.Service.AccesoClub;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.AccesoClubRegistroDTO;
import com.blue.open.Code.Entity.Db.AccesoClub;
import com.blue.open.Code.Entity.Db.ClubPresencia;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.AccesoClubTipoEnum;
import com.blue.open.Code.Entity.Enums.MemberEstadoEnum;
import com.blue.open.Code.Event.AccesoClubRegistroEvent;
import com.blue.open.Code.Mapper.AccesoClubMapper;
import com.blue.open.Code.Repository.AccesoClubRepository;
import com.blue.open.Code.Repository.ClubPresenciaRepository;
import com.blue.open.Code.Repository.MemberRepository;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Repository.UserRepository;
import com.blue.open.Core.Service.Auth.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccesoClubRegistroService {

    private static final int STATUS_ACTIVE = 0;
    private static final String ADVERTENCIA_MOROSO =
            "Socio moroso. El acceso se registró con advertencia.";

    private final AuthService authService;
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final AccesoClubRepository accesoClubRepository;
    private final ClubPresenciaRepository clubPresenciaRepository;
    private final AccesoClubMapper accesoClubMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final com.blue.open.Code.Service.Membresias.MembresiaAccesoService membresiaAccesoService;
    private final com.blue.open.Code.Service.Membresias.MembresiaSoporte membresiaSoporte;

    @Transactional
    public AccesoClubRegistroDTO registrar(String userUuid) {
        return registrar(userUuid, null, null);
    }

    /**
     * @param actividadUuid opcional: el ingreso es a una actividad (controla que el pase la incluya)
     * @param amenityUuid   opcional: el ingreso es a una instalación
     */
    @Transactional
    public AccesoClubRegistroDTO registrar(String userUuid, String actividadUuid, String amenityUuid) {
        authService.valid(List.of(PermissionEnum.CLUB_ACCESS_REGISTER));
        Company company = authService.getCurrentCompanyOrThrow();
        Long companyId = company.getId();

        User user = requireUserInCompany(userUuid, companyId);
        MemberAccesoContext memberContext = resolveMemberAccesoContext(user.getId(), companyId);

        boolean estabaPresente = clubPresenciaRepository
                .findByCompanyIdAndUserIdWithUser(companyId, user.getId())
                .map(ClubPresencia::isPresente)
                .orElse(false);

        AccesoClubTipoEnum tipo = estabaPresente ? AccesoClubTipoEnum.EGRESO : AccesoClubTipoEnum.INGRESO;
        boolean nuevoPresente = !estabaPresente;
        LocalDateTime fecha = LocalDateTime.now();

        // Pases: solo al entrar y para socios (los no socios siguen como hasta ahora).
        String advertenciaPase = null;
        if (tipo == AccesoClubTipoEnum.INGRESO && memberContext.member() != null) {
            com.blue.open.Code.Entity.Db.Actividad actividad = actividadUuid == null || actividadUuid.isBlank() ? null
                    : membresiaSoporte.porUuid(com.blue.open.Code.Entity.Db.Actividad.class, actividadUuid, companyId, "Actividad");
            com.blue.open.Code.Entity.Db.Amenity amenity = amenityUuid == null || amenityUuid.isBlank() ? null
                    : membresiaSoporte.porUuid(com.blue.open.Code.Entity.Db.Amenity.class, amenityUuid, companyId, "Instalación");
            var evaluacion = membresiaAccesoService.evaluar(company, memberContext.member(), actividad, amenity);
            if (!evaluacion.permitido()) {
                throw new CustomException(HttpStatus.FORBIDDEN, "Acceso rechazado: " + evaluacion.motivo(),
                        "AccesoClubRegistroService | pase | memberId: " + memberContext.member().getId());
            }
            advertenciaPase = evaluacion.advertencia();
        }

        AccesoClub acceso = new AccesoClub();
        acceso.setCompany(company);
        acceso.setUser(user);
        acceso.setMember(memberContext.member());
        acceso.setMemberEstado(memberContext.memberEstado());
        acceso.setTipo(tipo);
        acceso.setFecha(fecha);
        accesoClubRepository.save(acceso);

        actualizarPresencia(companyId, user.getId(), nuevoPresente, fecha);

        String advertencia = java.util.stream.Stream.of(memberContext.advertencia(), advertenciaPase)
                .filter(java.util.Objects::nonNull).reduce((a, b) -> a + " " + b).orElse(null);
        AccesoClubRegistroDTO registroDto = accesoClubMapper.toRegistroDto(acceso, nuevoPresente, advertencia);
        log.info("Publicando AccesoClubRegistroEvent para companyId: {}, userUuid: {}", companyId, registroDto.getUserUuid());
        eventPublisher.publishEvent(new AccesoClubRegistroEvent(this, registroDto, companyId));
        log.info("Evento publicado exitosamente");

        return registroDto;
    }

    private MemberAccesoContext resolveMemberAccesoContext(Long userId, Long companyId) {
        Optional<Member> memberOpt = memberRepository.findByUserIdAndCompanyIdWithDetails(userId, companyId);
        if (memberOpt.isEmpty()) {
            return MemberAccesoContext.withoutMember();
        }

        Member member = memberOpt.get();
        MemberEstadoEnum estado = member.getEstado();
        return switch (estado) {
            case ACTIVO -> new MemberAccesoContext(member, estado, null);
            case INHABILITADO, SUSPENDIDO, INACTIVO, MOROSO -> throw new CustomException(
                    HttpStatus.FORBIDDEN,
                    "Acceso no permitido para socios en estado " + formatEstado(estado),
                    "AccesoClubRegistroService | memberId: " + member.getId() + ", estado: " + estado);
        };
    }

    private String formatEstado(MemberEstadoEnum estado) {
        return estado.name().toLowerCase().replace('_', ' ');
    }

    private User requireUserInCompany(String userUuid, Long companyId) {
        if (userUuid == null || userUuid.isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Usuario requerido",
                    "AccesoClubRegistroService | userUuid vacío");
        }

        User user = userRepository.findFirstByUuidWithDetails(userUuid.trim());
        if (user == null || user.getStatus() == null || user.getStatus() != STATUS_ACTIVE) {
            throw new CustomException(HttpStatus.NOT_FOUND, "Usuario no encontrado",
                    "AccesoClubRegistroService | userUuid: " + userUuid);
        }

        if (user.getUserCompanyRoles() == null || user.getUserCompanyRoles().stream()
                .noneMatch(ucr -> ucr.getCompany() != null && companyId.equals(ucr.getCompany().getId()))) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "El usuario no pertenece a este gimnasio",
                    "AccesoClubRegistroService | userUuid: " + userUuid + ", companyId: " + companyId);
        }

        return user;
    }

    private void actualizarPresencia(Long companyId, Long userId, boolean presente, LocalDateTime ultimoAccesoAt) {
        if (clubPresenciaRepository.findByCompanyIdAndUserId(companyId, userId).isPresent()) {
            clubPresenciaRepository.updatePresenciaByCompanyIdAndUserId(
                    companyId, userId, presente, ultimoAccesoAt);
            return;
        }

        clubPresenciaRepository.insertPresenciaByCompanyIdAndUserId(
                UUID.randomUUID().toString(),
                companyId,
                userId,
                presente,
                ultimoAccesoAt);
    }

    private record MemberAccesoContext(Member member, MemberEstadoEnum memberEstado, String advertencia) {

        static MemberAccesoContext withoutMember() {
            return new MemberAccesoContext(null, null, null);
        }
    }
}
