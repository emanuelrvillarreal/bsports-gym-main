package com.blue.open.Code.Service.Categorias;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.CategoriaAltaCommand;
import com.blue.open.Code.Entity.DTO.CategoriaDTO;
import com.blue.open.Code.Entity.Db.Categoria;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Mapper.CategoriaMapper;
import com.blue.open.Code.Repository.CategoriaRepository;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoriaAltaService {

    @Autowired
    private AuthService authService;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private CategoriaMapper categoriaMapper;

    @Transactional
    public CategoriaDTO alta(CategoriaAltaCommand command) {
        authService.valid(List.of(PermissionEnum.CATEGORIA_CREATE));
        Company company = authService.getCurrentCompanyOrThrow();

        String nombre = command.getNombre().trim();
        if (categoriaRepository.existsNombreActivoEnCompany(company.getId(), nombre, null)) {
            throw new CustomException(HttpStatus.CONFLICT, "Ya existe una categoría con ese nombre",
                    "CategoriaAltaService | nombre duplicado: " + nombre);
        }

        Categoria categoria = new Categoria();
        categoria.setCompany(company);
        categoria.setNombre(nombre);
        categoria.setDescripcion(normalizeDescripcion(command.getDescripcion()));
        categoria.setEdadMinima(command.getEdadMinima());
        categoria.setEdadMaxima(command.getEdadMaxima());

        categoria = categoriaRepository.save(categoria);
        return categoriaMapper.modelToDto(categoria);
    }

    private String normalizeDescripcion(String descripcion) {
        if (descripcion == null) {
            return null;
        }
        String trimmed = descripcion.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
