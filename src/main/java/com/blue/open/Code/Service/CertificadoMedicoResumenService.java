package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.DTO.CertificadoMedicoResumenDTO;
import com.blue.open.Code.Entity.DTO.CertificadoMedicoResumenFilaDTO;
import com.blue.open.Code.Entity.DTO.CertificadoMedicoResumenRequestDTO;
import com.blue.open.Code.Entity.Db.CertificadoMedico;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.CertificadoMedicoEstadoEnum;
import com.blue.open.Code.Entity.Enums.CertificadoSituacionEnum;
import com.blue.open.Code.Repository.CertificadoMedicoRepository;
import com.blue.open.Code.Repository.InscripcionActividadRepository;
import com.blue.open.Code.Repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Listado de certificados médicos del club para el administrador: situación de cada socio, filtros y conteos.
 * Se arma en memoria sobre los socios del club (acotado por el límite de socios del plan) para poder filtrar por
 * situación y paginar con totales correctos.
 */
@Service
public class CertificadoMedicoResumenService {

    private static final int STATUS_ACTIVE = 0;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CertificadoMedicoRepository certificadoMedicoRepository;

    @Autowired
    private InscripcionActividadRepository inscripcionActividadRepository;

    @Transactional(readOnly = true)
    public CertificadoMedicoResumenDTO resumir(CertificadoMedicoResumenRequestDTO request, Long companyId) {
        CertificadoMedicoResumenRequestDTO req = request != null ? request : new CertificadoMedicoResumenRequestDTO();
        LocalDate hoy = LocalDate.now();

        List<Member> members = memberRepository.findVigentesConUsuario(companyId);
        List<Long> userIds = members.stream().map(m -> m.getUser().getId()).toList();

        Map<Long, List<CertificadoMedico>> certificadosPorUsuario = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (CertificadoMedico c : certificadoMedicoRepository.findByUser_IdInAndStatus(userIds, STATUS_ACTIVE)) {
                certificadosPorUsuario.computeIfAbsent(c.getUser().getId(), k -> new ArrayList<>()).add(c);
            }
        }

        Map<Long, Set<String>> actividadesPorMember = new HashMap<>();
        for (Object[] fila : inscripcionActividadRepository.findActividadesQueExigenCertificado(companyId)) {
            actividadesPorMember.computeIfAbsent((Long) fila[0], k -> new LinkedHashSet<>()).add((String) fila[1]);
        }

        Map<CertificadoSituacionEnum, Long> conteos = new EnumMap<>(CertificadoSituacionEnum.class);
        for (CertificadoSituacionEnum s : CertificadoSituacionEnum.values()) {
            conteos.put(s, 0L);
        }
        long bloqueados = 0;
        String busqueda = normalizar(req.getSearch());

        List<CertificadoMedicoResumenFilaDTO> filas = new ArrayList<>();
        for (Member member : members) {
            User user = member.getUser();
            List<CertificadoMedico> certificados = certificadosPorUsuario.getOrDefault(user.getId(), List.of());
            CertificadoSituacionEnum situacion = CertificadoMedicoVigenciaService.calcular(certificados, hoy);
            Set<String> exigen = actividadesPorMember.getOrDefault(member.getId(), Set.of());
            boolean bloqueado = !exigen.isEmpty() && !CertificadoMedicoVigenciaService.habilita(situacion);

            conteos.merge(situacion, 1L, Long::sum);
            if (bloqueado) {
                bloqueados++;
            }

            if (req.getSituacion() != null && req.getSituacion() != situacion) {
                continue;
            }
            if (req.isSoloQueLoNecesitan() && exigen.isEmpty()) {
                continue;
            }
            if (!busqueda.isEmpty() && !coincide(user, busqueda)) {
                continue;
            }
            filas.add(toFila(member, user, situacion, certificados, exigen, bloqueado, hoy));
        }

        filas.sort(Comparator
                .comparingInt((CertificadoMedicoResumenFilaDTO f) -> prioridad(f.getSituacion()))
                .thenComparing(f -> f.getFechaExpiracion() == null ? LocalDate.MAX : f.getFechaExpiracion())
                .thenComparing(f -> f.getApellido() == null ? "" : f.getApellido().toLowerCase(Locale.ROOT)));

        int size = Math.max(1, Math.min(req.getSize(), 100));
        int page = Math.max(0, req.getPage());
        int desde = Math.min(page * size, filas.size());
        int hasta = Math.min(desde + size, filas.size());
        Page<CertificadoMedicoResumenFilaDTO> pagina =
                new PageImpl<>(filas.subList(desde, hasta), PageRequest.of(page, size), filas.size());

        CertificadoMedicoResumenDTO dto = new CertificadoMedicoResumenDTO();
        dto.setConteos(conteos);
        dto.setBloqueados(bloqueados);
        dto.setFilas(pagina);
        return dto;
    }

    private CertificadoMedicoResumenFilaDTO toFila(Member member, User user, CertificadoSituacionEnum situacion,
                                                    List<CertificadoMedico> certificados, Set<String> exigen,
                                                    boolean bloqueado, LocalDate hoy) {
        CertificadoMedicoResumenFilaDTO fila = new CertificadoMedicoResumenFilaDTO();
        fila.setMemberUuid(member.getUuid());
        fila.setNombre(user.getNombre());
        fila.setApellido(user.getApellido());
        fila.setDni(user.getDni());
        fila.setImageUrl(user.getImageUrl());
        fila.setSituacion(situacion);
        LocalDate fecha = fechaRelevante(certificados);
        fila.setFechaExpiracion(fecha);
        fila.setDiasRestantes(fecha == null ? null : ChronoUnit.DAYS.between(hoy, fecha));
        fila.setActividadesQueLoExigen(List.copyOf(exigen));
        fila.setBloqueado(bloqueado);
        return fila;
    }

    /** La fecha de expiración más lejana entre los certificados confirmados o ya vencidos del socio. */
    private LocalDate fechaRelevante(List<CertificadoMedico> certificados) {
        LocalDate mayor = null;
        for (CertificadoMedico c : certificados) {
            if (c.getEstado() == CertificadoMedicoEstadoEnum.DENEGADO
                    || c.getEstado() == CertificadoMedicoEstadoEnum.PENDIENTE_VALIDACION) {
                continue;
            }
            if (mayor == null || c.getFechaExpiracion().isAfter(mayor)) {
                mayor = c.getFechaExpiracion();
            }
        }
        return mayor;
    }

    private int prioridad(CertificadoSituacionEnum situacion) {
        return switch (situacion) {
            case VENCIDO -> 0;
            case PENDIENTE_VALIDACION -> 1;
            case SIN_CERTIFICADO -> 2;
            case DENEGADO -> 3;
            case POR_VENCER -> 4;
            case VIGENTE -> 5;
        };
    }

    private boolean coincide(User user, String busqueda) {
        String texto = normalizar(user.getNombre() + " " + user.getApellido() + " " + (user.getDni() == null ? "" : user.getDni()));
        return texto.contains(busqueda);
    }

    private String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        return Normalizer.normalize(valor.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
