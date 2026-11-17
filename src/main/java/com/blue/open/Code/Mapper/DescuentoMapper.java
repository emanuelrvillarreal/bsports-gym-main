package com.blue.open.Code.Mapper;

import com.blue.open.Code.Entity.DTO.DescuentoAsignadoDTO;
import com.blue.open.Code.Entity.DTO.DescuentoDTO;
import com.blue.open.Code.Entity.Db.Descuento;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DescuentoMapper {

    public DescuentoDTO toDto(Descuento descuento) {
        DescuentoDTO dto = new DescuentoDTO();
        dto.setDescuentoUuid(descuento.getUuid());
        dto.setTitulo(descuento.getTitulo());
        dto.setTipoValor(descuento.getTipoValor());
        dto.setPorcentaje(descuento.getPorcentaje());
        dto.setMontoFijo(descuento.getMontoFijo());
        return dto;
    }

    public DescuentoAsignadoDTO toAsignadoDto(
            String asignacionUuid, Descuento descuento,
            LocalDate fechaDesde, LocalDate fechaHasta, boolean vigente) {

        DescuentoAsignadoDTO dto = new DescuentoAsignadoDTO();
        dto.setAsignacionUuid(asignacionUuid);
        dto.setDescuentoUuid(descuento.getUuid());
        dto.setTitulo(descuento.getTitulo());
        dto.setTipoValor(descuento.getTipoValor());
        dto.setPorcentaje(descuento.getPorcentaje());
        dto.setMontoFijo(descuento.getMontoFijo());
        dto.setFechaDesde(fechaDesde);
        dto.setFechaHasta(fechaHasta);
        dto.setVigente(vigente);
        return dto;
    }
}
