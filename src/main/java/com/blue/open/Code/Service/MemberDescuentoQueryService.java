package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.DTO.MemberDescuentoActividadDTO;
import com.blue.open.Code.Entity.DTO.MemberDescuentoPaginateRequestDTO;
import com.blue.open.Code.Entity.DTO.MemberDescuentosDTO;
import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.InscripcionActividad;
import com.blue.open.Code.Entity.Db.InscripcionActividadDescuento;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.MemberDescuento;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Mapper.DescuentoMapper;
import com.blue.open.Code.Repository.InscripcionActividadDescuentoRepository;
import com.blue.open.Code.Repository.MemberDescuentoRepository;
import com.blue.open.Code.Repository.MemberRepository;
import com.blue.open.Code.Repository.MemberSpecifications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MemberDescuentoQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberDescuentoRepository memberDescuentoRepository;

    @Autowired
    private InscripcionActividadDescuentoRepository inscripcionActividadDescuentoRepository;

    @Autowired
    private DescuentoMapper descuentoMapper;

    @Autowired
    private com.blue.open.Core.Service.Auth.AuthService authService;

    @Autowired
    private MemberAuthService memberAuthService;

    /**
     * Portal "Mis descuentos": los descuentos del socio que entró (cuota social y actividades). El titular ve
     * también los de su grupo familiar; un familiar ve solo los suyos.
     */
    @Transactional(readOnly = true)
    public List<MemberDescuentosDTO> portal() {
        authService.valid(List.of(com.blue.open.Code.Auth.PermissionEnum.PORTAL_FEE_VIEW));
        Long companyId = authService.getCurrentCompanyOrThrow().getId();
        Member actual = memberAuthService.getCurrentMemberOrThrow(companyId);
        List<Member> personas = actual.isTitular()
                ? memberRepository.findGrupoConPendientesByTitularId(companyId, actual.getId()).stream()
                        .filter(m -> m.getEstado() != com.blue.open.Code.Entity.Enums.MemberEstadoEnum.INACTIVO)
                        .toList()
                : List.of(actual);
        List<Long> ids = personas.stream().map(Member::getId).toList();
        if (ids.isEmpty()) return List.of();
        Map<Long, List<MemberDescuento>> cuotaSocial = memberDescuentoRepository.findActivasByMemberIdIn(ids).stream()
                .collect(Collectors.groupingBy(md -> md.getMember().getId()));
        Map<Long, List<InscripcionActividadDescuento>> actividades = inscripcionActividadDescuentoRepository
                .findActivasByMemberIdIn(ids).stream()
                .collect(Collectors.groupingBy(iad -> iad.getInscripcionActividad().getMember().getId()));
        return personas.stream()
                .sorted(java.util.Comparator.comparing((Member m) -> !m.getId().equals(actual.getId())))
                .map(m -> toDto(m, cuotaSocial, actividades))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<MemberDescuentosDTO> paginar(MemberDescuentoPaginateRequestDTO request, Long companyId) {
        MemberDescuentoPaginateRequestDTO req = request != null ? request : new MemberDescuentoPaginateRequestDTO();
        int page = Math.max(req.getPage(), 1);
        int size = req.getSize() <= 0 ? 10 : Math.min(req.getSize(), MAX_PAGE_SIZE);

        Specification<Member> spec = Specification.where(MemberSpecifications.forCompany(companyId))
                .and(MemberSpecifications.withSearchIncludingNumeroSocio(req.getSearch()))
                .and(MemberSpecifications.fetchUserAndTitular());

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(
                Sort.Order.asc("user.apellido"),
                Sort.Order.asc("user.nombre"),
                Sort.Order.asc("id")));

        Page<Member> memberPage = memberRepository.findAll(spec, pageable);

        List<Long> memberIds = memberPage.getContent().stream().map(Member::getId).toList();
        if (memberIds.isEmpty()) {
            return memberPage.map(member -> toDto(member, Map.of(), Map.of()));
        }

        Map<Long, List<MemberDescuento>> cuotaSocialPorMember = memberDescuentoRepository
                .findActivasByMemberIdIn(memberIds).stream()
                .collect(Collectors.groupingBy(md -> md.getMember().getId()));

        Map<Long, List<InscripcionActividadDescuento>> actividadDescuentosPorMember = inscripcionActividadDescuentoRepository
                .findActivasByMemberIdIn(memberIds).stream()
                .collect(Collectors.groupingBy(iad -> iad.getInscripcionActividad().getMember().getId()));

        return memberPage.map(member -> toDto(member, cuotaSocialPorMember, actividadDescuentosPorMember));
    }

    private MemberDescuentosDTO toDto(
            Member member,
            Map<Long, List<MemberDescuento>> cuotaSocialPorMember,
            Map<Long, List<InscripcionActividadDescuento>> actividadDescuentosPorMember) {

        LocalDate hoy = LocalDate.now();
        User user = member.getUser();

        MemberDescuentosDTO dto = new MemberDescuentosDTO();
        dto.setMemberUuid(member.getUuid());
        dto.setNombre(user != null ? (user.getNombre() + " " + user.getApellido()) : null);
        dto.setFotoUrl(user != null ? user.getImageUrl() : null);

        List<MemberDescuento> cuotaSocial = cuotaSocialPorMember.getOrDefault(member.getId(), List.of());
        dto.setCuotaSocial(cuotaSocial.stream()
                .map(md -> descuentoMapper.toAsignadoDto(md.getUuid(), md.getDescuento(),
                        md.getFechaDesde(), md.getFechaHasta(), md.estaVigente(hoy)))
                .toList());

        List<InscripcionActividadDescuento> actividadDescuentos =
                actividadDescuentosPorMember.getOrDefault(member.getId(), List.of());
        dto.setActividades(agruparPorActividad(actividadDescuentos, hoy));

        return dto;
    }

    private List<MemberDescuentoActividadDTO> agruparPorActividad(
            List<InscripcionActividadDescuento> actividadDescuentos, LocalDate hoy) {

        Map<Long, MemberDescuentoActividadDTO> porActividad = new LinkedHashMap<>();
        for (InscripcionActividadDescuento iad : actividadDescuentos) {
            InscripcionActividad inscripcion = iad.getInscripcionActividad();
            Actividad actividad = inscripcion.getActividad();

            MemberDescuentoActividadDTO actividadDto = porActividad.computeIfAbsent(actividad.getId(), id -> {
                MemberDescuentoActividadDTO nuevo = new MemberDescuentoActividadDTO();
                nuevo.setActividadUuid(actividad.getUuid());
                nuevo.setActividadNombre(actividad.getNombre());
                nuevo.setDescuentos(new ArrayList<>());
                return nuevo;
            });

            actividadDto.getDescuentos().add(descuentoMapper.toAsignadoDto(iad.getUuid(), iad.getDescuento(),
                    iad.getFechaDesde(), iad.getFechaHasta(), iad.estaVigente(hoy)));
        }
        return new ArrayList<>(porActividad.values());
    }
}
