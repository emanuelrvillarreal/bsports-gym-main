package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.DTO.ConfigurationDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Configuration;
import com.blue.open.Code.Mapper.ConfigurationMapper;
import com.blue.open.Code.Repository.CompanyRepository;
import com.blue.open.Code.Repository.ConfigurationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ConfiguracionService {
    @Autowired
    ConfigurationRepository repository;
    @Autowired
    ConfigurationMapper mapper;
    @Autowired
    CompanyRepository companyRepository;

    private Company resolveCompany(String companyUuid) {
        return companyUuid != null ? companyRepository.findFirstByUuidWithStatusIsActive(companyUuid) : null;
    }

    public List<ConfigurationDTO> getPublic() {
        return this.mapper.modelsToDtos(repository.findByGlobal(true));
    }

    public List<ConfigurationDTO> getAll(String companyUuid) {
        Company company = resolveCompany(companyUuid);
        if (company != null) {
            return this.mapper.modelsToDtos(repository.findByCompany(company));
        }
        return this.mapper.modelsToDtos((List<Configuration>) repository.findAll());
    }

    public Configuration getByKey(String key, String defaultValue, String companyUuid) {
        Company company = resolveCompany(companyUuid);
        Configuration configuration = company != null
                ? repository.findFirstByNameAndCompany(key, company)
                : repository.findFirstByName(key);
        if (configuration == null) {
            configuration = new Configuration();
            configuration.setName(key);
            configuration.setValue(defaultValue);
            configuration.setGlobal(true);
            configuration.setCompany(company);
        }
        return configuration;
    }

    @Transactional(readOnly = true)
    public ConfigurationDTO getByKeyDto(String key, String defaultValue, String companyUuid) {
        return mapper.modelToDto(getByKey(key, defaultValue, companyUuid));
    }

    public List<ConfigurationDTO> getByKeyList(List<String> keys, String companyUuid) {
        Company company = resolveCompany(companyUuid);
        List<Configuration> configuraciones = company != null
                ? repository.findByNameInAndCompany(keys, company)
                : repository.findByNameIn(keys);

        // Armar una lista con las que sí existen
        Set<String> existentes = configuraciones.stream()
                .map(Configuration::getName)
                .collect(Collectors.toSet());

        // Agregar las que no existen creando nuevas Configuration
        for (String key : keys) {
            if (!existentes.contains(key)) {
                Configuration nueva = new Configuration();
                nueva.setName(key);
                nueva.setGlobal(true);
                nueva.setCompany(company);
                configuraciones.add(nueva);
            }
        }

        return this.mapper.modelsToDtos(configuraciones);
    }

    @Transactional
    public List<ConfigurationDTO> saveOrCreate(List<ConfigurationDTO> nuevasConfiguraciones, String companyUuid) {
        Company company = resolveCompany(companyUuid);
        List<Configuration> configuracionesGuardar = new ArrayList<>();
        for (ConfigurationDTO nuevaConfiguracion: nuevasConfiguraciones) {
            Configuration configurationModel = company != null
                    ? repository.findFirstByNameAndCompany(nuevaConfiguracion.getName(), company)
                    : repository.findFirstByName(nuevaConfiguracion.getName());
            if (configurationModel == null) {
                configurationModel = new Configuration();
                configurationModel.setName(nuevaConfiguracion.getName());
                configurationModel.setCompany(company);
            }
            configurationModel.setGlobal(nuevaConfiguracion.isGlobal());
            configurationModel.setValue(nuevaConfiguracion.getValue());
            configuracionesGuardar.add(configurationModel);
        }
        repository.saveAll(configuracionesGuardar);

        return this.getByKeyList(
                nuevasConfiguraciones.stream()
                    .map(ConfigurationDTO::getName)
                    .collect(Collectors.toList()),
                companyUuid
        );
    }
}
