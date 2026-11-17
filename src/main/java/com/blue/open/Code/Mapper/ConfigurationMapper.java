package com.blue.open.Code.Mapper;

import com.blue.open.Code.Entity.DTO.ConfigurationDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Configuration;
import com.blue.open.Code.Repository.CompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ConfigurationMapper {
    @Autowired
    CompanyRepository companyRepository;

    public ConfigurationDTO modelToDto(Configuration model) {
        ConfigurationDTO dto = new ConfigurationDTO();
        dto.setName(model.getName());
        dto.setValue(model.getValue());
        dto.setGlobal(model.isGlobal());
        if (model.getCompany() != null) {
            dto.setCompanyUuid(model.getCompany().getUuid());
        }
        return dto;
    }

    public Configuration dtoToModel(ConfigurationDTO dto, Configuration model) {
        model.setName(dto.getName());
        model.setValue(dto.getValue());
        model.setGlobal(dto.isGlobal());
        if (dto.getCompanyUuid() != null) {
            Company company = companyRepository.findFirstByUuidWithStatusIsActive(dto.getCompanyUuid());
            model.setCompany(company);
        }
        return model;
    }

    public List<ConfigurationDTO> modelsToDtos(List<Configuration> models) {
        List<ConfigurationDTO> dtos = new ArrayList<>();
        for (Configuration model : models) {
            dtos.add(this.modelToDto(model));
        }
        return dtos;
    }
}
