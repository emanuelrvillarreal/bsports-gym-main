package com.blue.open.Code.Mapper;

import com.blue.open.Code.Entity.DTO.CuotaDetalleDTO;
import com.blue.open.Code.Entity.DTO.CuotaPaginateRowDTO;
import com.blue.open.Code.Entity.DTO.CuotaResumenDTO;
import com.blue.open.Code.Entity.Db.Cuota;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.CuotaEstadoEnum;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class CuotaMapper {

    private static final List<CuotaEstadoEnum> ESTADOS_DEUDA_VENCIDA = List.of(
            CuotaEstadoEnum.PENDIENTE,
            CuotaEstadoEnum.VENCIDA
    );

    private static final List<CuotaEstadoEnum> ESTADOS_POR_COBRAR = List.of(
            CuotaEstadoEnum.PENDIENTE,
            CuotaEstadoEnum.VENCIDA
    );

    public CuotaResumenDTO toResumen(Cuota cuota, LocalDate fechaCorte) {
        CuotaResumenDTO dto = new CuotaResumenDTO();
        dto.setCuotaUuid(cuota.getUuid());
        dto.setConcepto(cuota.getConcepto());
        dto.setTipo(cuota.getTipo());
        dto.setMonto(cuota.getMonto());
        dto.setMontoFinal(cuota.montoFinal());
        dto.setRecargo(cuota.getRecargo());
        dto.setVencimiento(cuota.getVencimiento());
        dto.setEstado(cuota.getEstado());
        dto.setFechaGeneracion(cuota.getFechaGeneracion());
        dto.setActividadId(cuota.getActividad() != null ? cuota.getActividad().getId() : null);
        boolean deudaVencida = esDeudaVencida(cuota, fechaCorte);
        dto.setDiasMora(deudaVencida ? calcularDiasMora(cuota.getVencimiento(), fechaCorte) : 0);
        return dto;
    }

    public CuotaDetalleDTO toDetalle(Cuota cuota, LocalDate fechaCorte) {
        CuotaDetalleDTO dto = new CuotaDetalleDTO();
        CuotaResumenDTO resumen = toResumen(cuota, fechaCorte);
        copyResumen(resumen, dto);
        mapPersona(dto, cuota.getPersona());
        mapTitular(dto, cuota.getTitular());
        mapAnulacion(dto, cuota);
        return dto;
    }

    public CuotaPaginateRowDTO toPaginateRow(Cuota cuota, LocalDate fechaCorte) {
        CuotaPaginateRowDTO row = new CuotaPaginateRowDTO();
        CuotaResumenDTO resumen = toResumen(cuota, fechaCorte);
        row.setCuotaUuid(resumen.getCuotaUuid());
        row.setConcepto(resumen.getConcepto());
        row.setTipo(resumen.getTipo());
        row.setMonto(resumen.getMonto());
        row.setDescuento(resumen.getDescuento());
        row.setMontoFinal(resumen.getMontoFinal());
        row.setRecargo(resumen.getRecargo());
        row.setVencimiento(resumen.getVencimiento());
        row.setEstado(resumen.getEstado());
        row.setDiasMora(resumen.getDiasMora());

        Member persona = cuota.getPersona();
        if (persona != null) {
            row.setPersonaMemberUuid(persona.getUuid());
            row.setPersonaEstado(persona.getEstado());
            User user = persona.getUser();
            if (user != null) {
                row.setPersonaNombre(user.getNombre());
                row.setPersonaApellido(user.getApellido());
            }
        }

        Member titular = cuota.getTitular();
        if (titular != null) {
            row.setTitularMemberUuid(titular.getUuid());
            User titularUser = titular.getUser();
            if (titularUser != null) {
                row.setTitularNombre(titularUser.getNombre());
                row.setTitularApellido(titularUser.getApellido());
                row.setTitularEmail(titularUser.getEmail());
                row.setTitularTelefono(titularUser.getPhone());
            }
        }
        return row;
    }

    public BigDecimal sumMontos(List<CuotaResumenDTO> cuotas) {
        return cuotas.stream()
                .map(CuotaResumenDTO::getMonto)
                .filter(m -> m != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal sumMontosDeuda(List<CuotaResumenDTO> cuotas) {
        return cuotas.stream()
                .filter(c -> c.getDiasMora() != null && c.getDiasMora() > 0)
                .map(CuotaResumenDTO::getMonto)
                .filter(m -> m != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal sumMontosPorPagar(List<CuotaResumenDTO> cuotas) {
        return cuotas.stream()
                .filter(c -> c.getEstado() != null && ESTADOS_POR_COBRAR.contains(c.getEstado()))
                .map(CuotaResumenDTO::getMonto)
                .filter(m -> m != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean esDeudaVencida(Cuota cuota, LocalDate fechaCorte) {
        if (cuota == null || cuota.getEstado() == null) {
            return false;
        }
        return ESTADOS_DEUDA_VENCIDA.contains(cuota.getEstado())
                && cuota.getVencimiento() != null
                && cuota.getVencimiento().isBefore(fechaCorte);
    }

    private int calcularDiasMora(LocalDate vencimiento, LocalDate fechaCorte) {
        if (vencimiento == null || !vencimiento.isBefore(fechaCorte)) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(vencimiento, fechaCorte);
    }

    private void copyResumen(CuotaResumenDTO from, CuotaDetalleDTO to) {
        to.setCuotaUuid(from.getCuotaUuid());
        to.setConcepto(from.getConcepto());
        to.setTipo(from.getTipo());
        to.setMonto(from.getMonto());
        to.setDescuento(from.getDescuento());
        to.setMontoFinal(from.getMontoFinal());
        to.setRecargo(from.getRecargo());
        to.setVencimiento(from.getVencimiento());
        to.setEstado(from.getEstado());
        to.setDiasMora(from.getDiasMora());
        to.setFechaGeneracion(from.getFechaGeneracion());
        to.setActividadId(from.getActividadId());
    }

    private void mapPersona(CuotaDetalleDTO dto, Member persona) {
        if (persona == null) {
            return;
        }
        dto.setPersonaMemberUuid(persona.getUuid());
        dto.setPersonaEstado(persona.getEstado());
        dto.setParentesco(persona.getParentesco());
        User user = persona.getUser();
        if (user != null) {
            dto.setPersonaNombre(user.getNombre());
            dto.setPersonaApellido(user.getApellido());
        }
    }

    private void mapTitular(CuotaDetalleDTO dto, Member titular) {
        if (titular == null) {
            return;
        }
        dto.setTitularMemberUuid(titular.getUuid());
        User user = titular.getUser();
        if (user != null) {
            dto.setTitularNombre(user.getNombre());
            dto.setTitularApellido(user.getApellido());
        }
    }

    private void mapAnulacion(CuotaDetalleDTO dto, Cuota cuota) {
        User anuladaPor = cuota.getAnuladaPor();
        if (anuladaPor != null) {
            dto.setAnuladaPorUuid(anuladaPor.getUuid());
            dto.setAnuladaPorNombre(anuladaPor.getNombre());
            dto.setAnuladaPorApellido(anuladaPor.getApellido());
        }
        dto.setAnuladaMotivo(cuota.getAnuladaMotivo());
        Timestamp anuladaAt = cuota.getAnuladaAt();
        if (anuladaAt != null) {
            dto.setAnuladaAt(anuladaAt.toLocalDateTime());
        }
    }
}
