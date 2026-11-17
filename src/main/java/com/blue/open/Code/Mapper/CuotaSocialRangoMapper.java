package com.blue.open.Code.Mapper;

import com.blue.open.Code.Entity.DTO.CuotaSocialRangoDTO;
import com.blue.open.Code.Entity.Db.CuotaSocialRango;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CuotaSocialRangoMapper {

    public CuotaSocialRangoDTO modelToDto(CuotaSocialRango rango) {
        if (rango == null) {
            return null;
        }
        CuotaSocialRangoDTO dto = new CuotaSocialRangoDTO();
        dto.setCuotaSocialRangoUuid(rango.getUuid());
        dto.setNombre(rango.getNombre());
        dto.setDescripcion(rango.getDescripcion());
        dto.setEdadHasta(rango.getEdadHasta());
        dto.setImporte(rango.getImporte());
        dto.setEsDefault(rango.getEsDefault());
        return dto;
    }

    public List<CuotaSocialRangoDTO> modelsToDtos(List<CuotaSocialRango> rangos) {
        if (rangos == null) {
            return List.of();
        }
        return rangos.stream().map(this::modelToDto).toList();
    }
}
