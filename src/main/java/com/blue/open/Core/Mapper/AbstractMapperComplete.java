package com.blue.open.Core.Mapper;

import com.blue.open.Core.Entity.Db.AbstractEntity;
import com.blue.open.Core.Exceptions.CustomException;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractMapperComplete<T extends AbstractEntity, DTO, PAGINATEDTO> {

    /**
     * Convierte un entity a DTO completo
     */
    public abstract DTO modelToDto(T model);

    /**
     * Convierte un DTO a entity (usando un modelo existente o nuevo)
     */
    public abstract T dtoToModel(DTO dto, T model);

    /**
     * Convierte un entity a DTO de paginación
     */
    public abstract PAGINATEDTO modelToPaginateDto(T model);

    /**
     * Convierte una lista de entities a lista de DTOs completos
     * (implementación genérica)
     */
    public List<DTO> modelsToDtos(List<T> models) {
        List<DTO> dtos = new ArrayList<>();
        if (models != null) {
            for (T model : models) {
                dtos.add(this.modelToDto(model));
            }
        }
        return dtos;
    }

    /**
     * Convierte una lista de DTOs a lista de entities
     * (implementación genérica)
     */
    public List<T> dtosToModels(List<DTO> dtos) {
        List<T> models = new ArrayList<>();
        if (dtos != null) {
            for (DTO dto : dtos) {
                models.add(this.dtoToModel(dto, null));
            }
        }
        return models;
    }

    /**
     * Convierte una lista de entities a lista de DTOs de paginación
     * (implementación genérica)
     */
    public List<PAGINATEDTO> modelsToPaginateDtos(List<T> models) {
        List<PAGINATEDTO> dtos = new ArrayList<>();
        try {
            if (models != null) {
                for (T model : models) {
                    try {
                        dtos.add(this.modelToPaginateDto(model));
                    } catch (CustomException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new CustomException("Error convertir un registro del paginado", "error en modelsToPaginateDtos | registro id: "+model.getId()+" | " + e.getMessage());
                    }
                }
            }
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Error convertir los datos del paginado", "error en modelsToPaginateDtos | " + e.getMessage());
        }
        return dtos;
    }
}
