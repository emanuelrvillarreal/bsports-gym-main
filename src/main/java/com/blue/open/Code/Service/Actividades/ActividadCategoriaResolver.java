package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Entity.Db.Categoria;
import com.blue.open.Code.Repository.CategoriaRepository;
import com.blue.open.Core.Exceptions.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ActividadCategoriaResolver {

    @Autowired
    private CategoriaRepository categoriaRepository;

    public Set<Categoria> resolve(Long companyId, List<String> categoriaUuids) {
        Set<Categoria> categorias = new LinkedHashSet<>();
        if (categoriaUuids == null || categoriaUuids.isEmpty()) {
            return categorias;
        }

        Set<String> uuidsUnicos = new LinkedHashSet<>();
        for (String categoriaUuid : categoriaUuids) {
            uuidsUnicos.add(categoriaUuid.trim());
        }

        Map<String, Categoria> categoriaPorUuid = categoriaRepository
                .findByUuidInAndCompanyIdAndStatusActive(uuidsUnicos, companyId).stream()
                .collect(Collectors.toMap(Categoria::getUuid, categoria -> categoria));

        List<String> uuidsNoEncontrados = uuidsUnicos.stream()
                .filter(uuid -> !categoriaPorUuid.containsKey(uuid))
                .toList();
        if (!uuidsNoEncontrados.isEmpty()) {
            throw new CustomException(HttpStatus.NOT_FOUND, "Categoría no encontrada",
                    "ActividadCategoriaResolver | categoriaUuids: " + uuidsNoEncontrados);
        }

        for (String categoriaUuid : uuidsUnicos) {
            categorias.add(categoriaPorUuid.get(categoriaUuid));
        }
        return categorias;
    }
}
