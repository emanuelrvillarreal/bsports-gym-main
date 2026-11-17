package com.blue.open.Code.Service.Tesoreria;

import com.blue.open.Code.Entity.DTO.MemberEstadoRecalculoResultDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Enums.MemberEstadoEnum;
import com.blue.open.Code.Repository.CompanyRepository;
import com.blue.open.Code.Repository.CuotaRepository;
import com.blue.open.Code.Repository.MemberRepository;
import com.blue.open.Code.Service.Actividades.InscripcionActividadBajaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class MemberEstadoRecalculoService {

    private static final int DEFAULT_DIAS_VENCIMIENTO = 31;
    private static final int DEFAULT_DIAS_INHABILITACION = 91;

    private static final Set<MemberEstadoEnum> ESTADOS_AUTO_GESTIONADOS = EnumSet.of(
            MemberEstadoEnum.ACTIVO,
            MemberEstadoEnum.MOROSO,
            MemberEstadoEnum.INHABILITADO
    );

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CuotaRepository cuotaRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private InscripcionActividadBajaService inscripcionActividadBajaService;

    @Transactional
    public MemberEstadoRecalculoResultDTO recalcularGrupoPorTitular(Long companyId, Long titularMemberId) {
        return recalcularGrupoPorTitular(companyId, titularMemberId, LocalDate.now());
    }

    @Transactional
    public MemberEstadoRecalculoResultDTO recalcularGrupoPorTitular(
            Long companyId, Long titularMemberId, LocalDate fechaCorte) {

        if (companyId == null || titularMemberId == null) {
            return new MemberEstadoRecalculoResultDTO();
        }
        Company company = companyRepository.findById(companyId).orElse(null);
        if (company == null) {
            return new MemberEstadoRecalculoResultDTO();
        }
        return recalcularGrupoPorTitularConCompany(company, titularMemberId, fechaCorte);
    }

    public MemberEstadoRecalculoResultDTO recalcularGruposPorTitularIds(
            Long companyId, Collection<Long> titularMemberIds) {

        MemberEstadoRecalculoResultDTO total = new MemberEstadoRecalculoResultDTO();
        if (companyId == null || titularMemberIds == null || titularMemberIds.isEmpty()) {
            return total;
        }

        Company company = companyRepository.findById(companyId).orElse(null);
        if (company == null) {
            return total;
        }

        LocalDate fechaCorte = LocalDate.now();
        for (Long titularMemberId : titularMemberIds) {
            if (titularMemberId == null) {
                continue;
            }
            MemberEstadoRecalculoResultDTO partial = transactionTemplate.execute(
                    status -> recalcularGrupoPorTitularConCompany(company, titularMemberId, fechaCorte));
            total.merge(partial);
        }
        return total;
    }

    public MemberEstadoRecalculoResultDTO recalcularPorCompany(Long companyId) {
        return recalcularPorCompany(companyId, LocalDate.now());
    }

    public MemberEstadoRecalculoResultDTO recalcularPorCompany(Long companyId, LocalDate fechaCorte) {
        MemberEstadoRecalculoResultDTO total = new MemberEstadoRecalculoResultDTO();
        if (companyId == null) {
            return total;
        }

        Company company = companyRepository.findById(companyId).orElse(null);
        if (company == null) {
            return total;
        }

        Set<Long> titularIds = cuotaRepository.findTitularIdsConCuotaVencida(companyId);

        for (Long titularMemberId : titularIds) {
            MemberEstadoRecalculoResultDTO partial = transactionTemplate.execute(
                    status -> recalcularGrupoPorTitularConCompany(company, titularMemberId, fechaCorte));
            total.merge(partial);
        }
        return total;
    }

    private MemberEstadoRecalculoResultDTO recalcularGrupoPorTitularConCompany(
            Company company, Long titularMemberId, LocalDate fechaCorte) {

        MemberEstadoRecalculoResultDTO result = new MemberEstadoRecalculoResultDTO();
        List<Member> grupo = memberRepository.findGrupoByTitularId(company.getId(), titularMemberId);
        if (grupo.isEmpty()) {
            return result;
        }
        result.setGruposProcesados(1);
        applyRecalculoToGrupo(grupo, company, titularMemberId, fechaCorte, result);
        return result;
    }

    private void applyRecalculoToGrupo(
            List<Member> grupo,
            Company company,
            Long titularMemberId,
            LocalDate fechaCorte,
            MemberEstadoRecalculoResultDTO result) {

        int diasVencimientoMoroso = resolveDiasVencimientoMoroso(company);
        int diasInhabilitacion = resolveDiasInhabilitacion(company);

        Optional<LocalDate> vencimientoMasAntiguoOpt = cuotaRepository.findVencimientoCuotaVencidaMasAntiguaGrupo(
                company.getId(), titularMemberId);

        MemberEstadoEnum estadoDestinoGrupo = resolveEstadoDestinoGrupo(
                vencimientoMasAntiguoOpt, diasVencimientoMoroso, diasInhabilitacion, fechaCorte);

        List<Member> autoGestionados = grupo.stream()
                .filter(m -> ESTADOS_AUTO_GESTIONADOS.contains(m.getEstado()))
                .toList();

        List<Member> modificados = new ArrayList<>();
        boolean nuevaInhabilitacionEnGrupo = false;
        for (Member member : autoGestionados) {
            if (member.getEstado() != estadoDestinoGrupo) {
                if (estadoDestinoGrupo == MemberEstadoEnum.INHABILITADO
                        && member.getEstado() != MemberEstadoEnum.INHABILITADO) {
                    nuevaInhabilitacionEnGrupo = true;
                }
                applyEstadoChange(member, estadoDestinoGrupo, result);
                modificados.add(member);
            }
        }

        if (!modificados.isEmpty()) {
            memberRepository.saveAll(modificados);
        }

        if (nuevaInhabilitacionEnGrupo) {
            inscripcionActividadBajaService.bajaTodasActivasDelGrupo(grupo, company.getId());
        }
    }

    private MemberEstadoEnum resolveEstadoDestinoGrupo(
            Optional<LocalDate> vencimientoMasAntiguo,
            int diasVencimientoMoroso,
            int diasInhabilitacion,
            LocalDate fechaCorte) {

        if (vencimientoMasAntiguo.isEmpty()) {
            return MemberEstadoEnum.ACTIVO;
        }
        long diasDesdeVencimiento = ChronoUnit.DAYS.between(vencimientoMasAntiguo.get(), fechaCorte);
        if (diasDesdeVencimiento >= diasInhabilitacion) {
            return MemberEstadoEnum.INHABILITADO;
        }
        if (diasDesdeVencimiento >= diasVencimientoMoroso) {
            return MemberEstadoEnum.MOROSO;
        }
        return MemberEstadoEnum.ACTIVO;
    }

    private void applyEstadoChange(Member member, MemberEstadoEnum estadoDestino, MemberEstadoRecalculoResultDTO result) {
        MemberEstadoEnum estadoAnterior = member.getEstado();
        member.setEstado(estadoDestino);
        result.setMiembrosActualizados(result.getMiembrosActualizados() + 1);

        if (estadoDestino == MemberEstadoEnum.INHABILITADO && estadoAnterior != MemberEstadoEnum.INHABILITADO) {
            member.setGrupoInhabilitadoAt(new Timestamp(System.currentTimeMillis()));
            result.setInhabilitaciones(result.getInhabilitaciones() + 1);
        }

        if (estadoDestino == MemberEstadoEnum.ACTIVO
                && (estadoAnterior == MemberEstadoEnum.INHABILITADO || estadoAnterior == MemberEstadoEnum.MOROSO)) {
            member.setGrupoInhabilitadoAt(null);
            result.setRecuperaciones(result.getRecuperaciones() + 1);
        }
    }

    private int resolveDiasVencimientoMoroso(Company company) {
        Integer dias = company.getDiasVencimientoMoroso();
        return dias != null && dias > 0 ? dias : DEFAULT_DIAS_VENCIMIENTO;
    }

    private int resolveDiasInhabilitacion(Company company) {
        Integer dias = company.getDiasInhabilitacion();
        return dias != null && dias > 0 ? dias : DEFAULT_DIAS_INHABILITACION;
    }
}
