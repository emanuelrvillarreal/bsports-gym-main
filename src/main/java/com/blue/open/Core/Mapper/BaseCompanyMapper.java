package com.blue.open.Core.Mapper;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Core.Entity.DTO.BaseCompanyDTO;
import com.blue.open.Core.Exceptions.CustomException;
public abstract class BaseCompanyMapper<DTO extends BaseCompanyDTO>
        extends AbstractMapperComplete<Company, DTO, DTO> {

    @Override
    public DTO modelToDto(Company model) {
        try {
            DTO dto = createDto();
            dto.setAbstract(model);
            dto.setName(model.getName());
            dto.setDescription(model.getDescription());
            dto.setPath(model.getPath());
            dto.setCode(model.getCode());
            return dto;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Error al convertir los datos",
                    "Error en el mapper modelToDto " + e.getMessage());
        }
    }

    @Override
    public Company dtoToModel(DTO dto, Company model) {
        if (model == null) {
            model = createModel();
        }
        model.setAbstract(dto);
        model.setName(dto.getName());
        model.setDescription(dto.getDescription());
        model.setPath(dto.getPath());
        model.setCode(dto.getCode());
        return model;
    }

    @Override
    public DTO modelToPaginateDto(Company model) {
        return modelToDto(model);
    }

    protected abstract DTO createDto();

    protected abstract Company createModel();
}
