package com.blue.open.Code.Mapper;

import com.blue.open.Code.Entity.DTO.CategoriaDTO;
import com.blue.open.Code.Entity.Db.Categoria;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoriaMapper {

    public CategoriaDTO modelToDto(Categoria categoria) {
        if (categoria == null) {
            return null;
        }
        CategoriaDTO dto = new CategoriaDTO();
        dto.setCategoriaUuid(categoria.getUuid());
        dto.setNombre(categoria.getNombre());
        dto.setDescripcion(categoria.getDescripcion());
        dto.setEdadMinima(categoria.getEdadMinima());
        dto.setEdadMaxima(categoria.getEdadMaxima());
        return dto;
    }

    public List<CategoriaDTO> modelsToDtos(List<Categoria> categorias) {
        if (categorias == null) {
            return List.of();
        }
        return categorias.stream().map(this::modelToDto).toList();
    }
}
