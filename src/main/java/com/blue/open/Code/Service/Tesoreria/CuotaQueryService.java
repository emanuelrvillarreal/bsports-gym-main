package com.blue.open.Code.Service.Tesoreria;

import com.blue.open.Code.Entity.DTO.CuotaDetalleDTO;
import com.blue.open.Code.Entity.DTO.CuotaFiltroDTO;
import com.blue.open.Code.Entity.DTO.CuotaGrupoTitularDTO;
import com.blue.open.Code.Entity.DTO.CuotaPaginateRequestDTO;
import com.blue.open.Code.Entity.DTO.CuotaPaginateRowDTO;
import com.blue.open.Code.Entity.DTO.CuotaPersonaGrupoDTO;
import com.blue.open.Code.Entity.DTO.CuotaPersonaListDTO;
import com.blue.open.Code.Entity.DTO.CuotaResumenDTO;
import com.blue.open.Code.Entity.Db.Cuota;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.CuotaOrdenEnum;
import com.blue.open.Code.Mapper.CuotaMapper;
import com.blue.open.Code.Repository.CuotaRepository;
import com.blue.open.Code.Repository.CuotaSpecifications;
import com.blue.open.Code.Repository.MemberRepository;
import com.blue.open.Core.Exceptions.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CuotaQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    @Autowired
    private CuotaRepository cuotaRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CuotaMapper cuotaMapper;

    @Transactional(readOnly = true)
    public Page<CuotaPaginateRowDTO> paginar(CuotaPaginateRequestDTO request, Long companyId) {
        CuotaPaginateRequestDTO req = request != null ? request : new CuotaPaginateRequestDTO();
        int page = Math.max(req.getPage(), 1);
        int size = req.getSize() <= 0 ? 10 : Math.min(req.getSize(), MAX_PAGE_SIZE);

        Long titularId = resolveOptionalMemberId(req.getTitularMemberUuid(), companyId);
        Long personaId = resolveOptionalMemberId(req.getPersonaMemberUuid(), companyId);
        LocalDate fechaCorte = LocalDate.now();

        Specification<Cuota> spec = Specification.where(CuotaSpecifications.forCompany(companyId))
                .and(CuotaSpecifications.withPaginateRequest(req, titularId, personaId, fechaCorte));

        Pageable pageable = PageRequest.of(page - 1, size, toSort(req.getOrden()));
        Page<Cuota> pageResult = cuotaRepository.findAll(spec, pageable);

        return pageResult.map(c -> cuotaMapper.toPaginateRow(c, fechaCorte));
    }

    @Transactional(readOnly = true)
    public CuotaPersonaListDTO listarPorPersona(Long personaMemberId, CuotaFiltroDTO filtro, Long companyId) {
        Member persona = memberRepository.findByIdWithTitular(personaMemberId)
                .filter(m -> m.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Socio no encontrado",
                        "CuotaQueryService | personaMemberId: " + personaMemberId));

        List<Cuota> cuotas = buscarCuotas(companyId, filtro, personaMemberId, null);
        return buildPersonaList(persona, cuotas);
    }

    @Transactional(readOnly = true)
    public CuotaGrupoTitularDTO listarGrupoPorTitular(Long titularMemberId, CuotaFiltroDTO filtro, Long companyId) {
        Member titular = memberRepository.findByIdWithTitular(titularMemberId)
                .filter(m -> m.getCompany().getId().equals(companyId) && m.isTitular())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Socio titular no encontrado",
                        "CuotaQueryService | titularMemberId: " + titularMemberId));

        List<Member> grupo = memberRepository.findGrupoByTitularId(companyId, titularMemberId);
        List<Cuota> cuotas = buscarCuotas(companyId, filtro, null, titularMemberId);

        Map<Long, List<CuotaResumenDTO>> cuotasPorPersona = agruparResumenes(cuotas);

        CuotaGrupoTitularDTO result = new CuotaGrupoTitularDTO();
        result.setTitularMemberUuid(titular.getUuid());
        User titularUser = titular.getUser();
        if (titularUser != null) {
            result.setTitularNombre(titularUser.getNombre());
            result.setTitularApellido(titularUser.getApellido());
        }

        List<CuotaPersonaGrupoDTO> personas = grupo.stream()
                .sorted(Comparator
                        .comparing((Member m) -> !m.isTitular())
                        .thenComparing(m -> apellidoOrEmpty(m), String.CASE_INSENSITIVE_ORDER))
                .map(member -> toPersonaGrupo(member, cuotasPorPersona.getOrDefault(member.getId(), List.of())))
                .collect(Collectors.toList());

        result.setPersonas(personas);
        result.setMontoTotalGrupo(personas.stream()
                .map(CuotaPersonaGrupoDTO::getSubtotalPersona)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        result.setMontoDeudaGrupo(personas.stream()
                .map(CuotaPersonaGrupoDTO::getSubtotalDeudaPersona)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        result.setTotalPorPagarGrupo(personas.stream()
                .map(CuotaPersonaGrupoDTO::getTotalPorPagar)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return result;
    }

    @Transactional(readOnly = true)
    public CuotaDetalleDTO obtenerPorUuid(String cuotaUuid, Long companyId) {
        Cuota cuota = cuotaRepository.findByUuidAndCompanyIdWithDetails(cuotaUuid, companyId);
        if (cuota == null) {
            throw new CustomException(HttpStatus.NOT_FOUND, "Cuota no encontrada",
                    "CuotaQueryService | uuid: " + cuotaUuid);
        }
        return cuotaMapper.toDetalle(cuota, LocalDate.now());
    }

    private List<Cuota> buscarCuotas(
            Long companyId,
            CuotaFiltroDTO filtro,
            Long personaMemberId,
            Long titularMemberId) {
        LocalDate fechaCorte = LocalDate.now();
        CuotaFiltroDTO f = filtro != null ? filtro : new CuotaFiltroDTO();

        Specification<Cuota> spec = Specification.where(CuotaSpecifications.forCompany(companyId))
                .and(CuotaSpecifications.withFiltro(f, fechaCorte))
                .and(CuotaSpecifications.fetchPersonaAndTitular());

        if (personaMemberId != null) {
            spec = spec.and(CuotaSpecifications.withPersonaMemberId(personaMemberId));
        }
        if (titularMemberId != null) {
            spec = spec.and(CuotaSpecifications.withTitularMemberId(titularMemberId));
        }

        return cuotaRepository.findAll(spec, toSort(f.getOrden()));
    }

    private CuotaPersonaListDTO buildPersonaList(Member persona, List<Cuota> cuotas) {
        LocalDate fechaCorte = LocalDate.now();
        List<CuotaResumenDTO> resumenes = cuotas.stream()
                .map(c -> cuotaMapper.toResumen(c, fechaCorte))
                .collect(Collectors.toList());

        CuotaPersonaListDTO dto = new CuotaPersonaListDTO();
        dto.setPersonaMemberUuid(persona.getUuid());
        dto.setEstadoMember(persona.getEstado());
        dto.setParentesco(persona.getParentesco());
        User user = persona.getUser();
        if (user != null) {
            dto.setNombre(user.getNombre());
            dto.setApellido(user.getApellido());
        }
        if (!persona.isTitular() && persona.getTitular() != null) {
            dto.setTitularMemberUuid(persona.getTitular().getUuid());
        } else {
            dto.setTitularMemberUuid(persona.getUuid());
        }
        dto.setCuotas(resumenes);
        dto.setSubtotalPersona(cuotaMapper.sumMontos(resumenes));
        dto.setSubtotalDeudaPersona(cuotaMapper.sumMontosDeuda(resumenes));
        dto.setTotalPorPagar(cuotaMapper.sumMontosPorPagar(resumenes));
        return dto;
    }

    private CuotaPersonaGrupoDTO toPersonaGrupo(Member member, List<CuotaResumenDTO> cuotas) {
        CuotaPersonaGrupoDTO dto = new CuotaPersonaGrupoDTO();
        dto.setPersonaMemberUuid(member.getUuid());
        dto.setEstadoMember(member.getEstado());
        dto.setParentesco(member.getParentesco());
        User user = member.getUser();
        if (user != null) {
            dto.setNombre(user.getNombre());
            dto.setApellido(user.getApellido());
        }
        dto.setCuotas(new ArrayList<>(cuotas));
        dto.setSubtotalPersona(cuotaMapper.sumMontos(cuotas));
        dto.setSubtotalDeudaPersona(cuotaMapper.sumMontosDeuda(cuotas));
        dto.setTotalPorPagar(cuotaMapper.sumMontosPorPagar(cuotas));
        return dto;
    }

    private Map<Long, List<CuotaResumenDTO>> agruparResumenes(List<Cuota> cuotas) {
        LocalDate fechaCorte = LocalDate.now();
        Map<Long, List<CuotaResumenDTO>> map = new LinkedHashMap<>();
        for (Cuota cuota : cuotas) {
            Long personaId = cuota.getPersona().getId();
            map.computeIfAbsent(personaId, k -> new ArrayList<>())
                    .add(cuotaMapper.toResumen(cuota, fechaCorte));
        }
        return map;
    }

    private Long resolveOptionalMemberId(String memberUuid, Long companyId) {
        if (memberUuid == null || memberUuid.isBlank()) {
            return null;
        }
        Member member = memberRepository.findByUuidAndCompanyIdWithDetails(memberUuid, companyId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Socio no encontrado",
                        "CuotaQueryService | uuid: " + memberUuid));
        return member.getId();
    }

    private Sort toSort(CuotaOrdenEnum orden) {
        if (orden == null) {
            return Sort.by(Sort.Direction.ASC, "vencimiento");
        }
        return switch (orden) {
            case VENCIMIENTO_DESC -> Sort.by(Sort.Direction.DESC, "vencimiento");
            case PERSONA_APELLIDO -> Sort.by(
                    Sort.Order.asc("persona.user.apellido"),
                    Sort.Order.asc("persona.user.nombre"),
                    Sort.Order.asc("vencimiento"));
            default -> Sort.by(Sort.Direction.ASC, "vencimiento");
        };
    }

    private String apellidoOrEmpty(Member member) {
        if (member.getUser() == null || member.getUser().getApellido() == null) {
            return "";
        }
        return member.getUser().getApellido();
    }
}
