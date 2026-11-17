package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.DTO.DescuentoAsignadoDTO;
import com.blue.open.Code.Entity.DTO.DescuentoFinalizarCommand;
import com.blue.open.Code.Entity.DTO.InscripcionActividadDescuentoCommand;
import com.blue.open.Code.Entity.DTO.MemberDescuentoCommand;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Descuento;
import com.blue.open.Code.Entity.Db.InscripcionActividad;
import com.blue.open.Code.Entity.Db.InscripcionActividadDescuento;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.MemberDescuento;
import com.blue.open.Code.Mapper.DescuentoMapper;
import com.blue.open.Code.Repository.DescuentoRepository;
import com.blue.open.Code.Repository.InscripcionActividadDescuentoRepository;
import com.blue.open.Code.Repository.InscripcionActividadRepository;
import com.blue.open.Code.Repository.MemberDescuentoRepository;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class DescuentoAsignacionService {

    @Autowired
    private AuthService authService;

    @Autowired
    private MemberAuthService memberAuthService;

    @Autowired
    private DescuentoRepository descuentoRepository;

    @Autowired
    private MemberDescuentoRepository memberDescuentoRepository;

    @Autowired
    private InscripcionActividadRepository inscripcionActividadRepository;

    @Autowired
    private InscripcionActividadDescuentoRepository inscripcionActividadDescuentoRepository;

    @Autowired
    private DescuentoMapper descuentoMapper;

    @Transactional
    public DescuentoAsignadoDTO asignarASocio(String memberUuid, MemberDescuentoCommand command) {
        Company company = authService.getCurrentCompanyOrThrow();
        Member member = memberAuthService.resolveMemberByUuid(memberUuid, company.getId());
        Descuento descuento = resolverDescuento(command.getDescuentoUuid(), company.getId());

        MemberDescuento asignacion = MemberDescuento.asignar(
                member, descuento, command.getFechaDesde(), command.getFechaHasta());
        memberDescuentoRepository.save(asignacion);

        return descuentoMapper.toAsignadoDto(asignacion.getUuid(), descuento,
                asignacion.getFechaDesde(), asignacion.getFechaHasta(), asignacion.estaVigente(LocalDate.now()));
    }

    @Transactional
    public void bajaMemberDescuento(String asignacionUuid) {
        Company company = authService.getCurrentCompanyOrThrow();
        MemberDescuento asignacion = resolverMemberDescuento(asignacionUuid, company.getId());
        memberDescuentoRepository.delete(asignacion);
    }

    @Transactional
    public DescuentoAsignadoDTO finalizarMemberDescuento(String asignacionUuid, DescuentoFinalizarCommand command) {
        Company company = authService.getCurrentCompanyOrThrow();
        MemberDescuento asignacion = resolverMemberDescuento(asignacionUuid, company.getId());

        asignacion.finalizar(command.getFecha());
        memberDescuentoRepository.save(asignacion);

        return descuentoMapper.toAsignadoDto(asignacion.getUuid(), asignacion.getDescuento(),
                asignacion.getFechaDesde(), asignacion.getFechaHasta(), asignacion.estaVigente(LocalDate.now()));
    }

    @Transactional
    public DescuentoAsignadoDTO asignarAInscripcion(
            String inscripcionActividadUuid, InscripcionActividadDescuentoCommand command) {
        Company company = authService.getCurrentCompanyOrThrow();
        InscripcionActividad inscripcion = inscripcionActividadRepository
                .findByUuidAndCompanyIdAndStatusActive(inscripcionActividadUuid, company.getId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Inscripción no encontrada",
                        "DescuentoAsignacionService | inscripcionActividadUuid: " + inscripcionActividadUuid));
        Descuento descuento = resolverDescuento(command.getDescuentoUuid(), company.getId());

        InscripcionActividadDescuento asignacion = InscripcionActividadDescuento.asignar(
                inscripcion, descuento, command.getFechaDesde(), command.getFechaHasta());
        inscripcionActividadDescuentoRepository.save(asignacion);

        return descuentoMapper.toAsignadoDto(asignacion.getUuid(), descuento,
                asignacion.getFechaDesde(), asignacion.getFechaHasta(), asignacion.estaVigente(LocalDate.now()));
    }

    @Transactional
    public void bajaInscripcionActividadDescuento(String asignacionUuid) {
        Company company = authService.getCurrentCompanyOrThrow();
        InscripcionActividadDescuento asignacion = resolverInscripcionActividadDescuento(asignacionUuid, company.getId());
        inscripcionActividadDescuentoRepository.delete(asignacion);
    }

    @Transactional
    public DescuentoAsignadoDTO finalizarInscripcionActividadDescuento(
            String asignacionUuid, DescuentoFinalizarCommand command) {
        Company company = authService.getCurrentCompanyOrThrow();
        InscripcionActividadDescuento asignacion = resolverInscripcionActividadDescuento(asignacionUuid, company.getId());

        asignacion.finalizar(command.getFecha());
        inscripcionActividadDescuentoRepository.save(asignacion);

        return descuentoMapper.toAsignadoDto(asignacion.getUuid(), asignacion.getDescuento(),
                asignacion.getFechaDesde(), asignacion.getFechaHasta(), asignacion.estaVigente(LocalDate.now()));
    }

    private Descuento resolverDescuento(String descuentoUuid, Long companyId) {
        return descuentoRepository.findByUuidAndCompanyIdAndStatusActive(descuentoUuid, companyId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Descuento no encontrado",
                        "DescuentoAsignacionService | descuentoUuid: " + descuentoUuid));
    }

    private MemberDescuento resolverMemberDescuento(String asignacionUuid, Long companyId) {
        return memberDescuentoRepository.findByUuidAndCompanyIdAndStatusActive(asignacionUuid, companyId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Asignación no encontrada",
                        "DescuentoAsignacionService | asignacionUuid: " + asignacionUuid));
    }

    private InscripcionActividadDescuento resolverInscripcionActividadDescuento(String asignacionUuid, Long companyId) {
        return inscripcionActividadDescuentoRepository.findByUuidAndCompanyIdAndStatusActive(asignacionUuid, companyId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Asignación no encontrada",
                        "DescuentoAsignacionService | asignacionUuid: " + asignacionUuid));
    }
}
