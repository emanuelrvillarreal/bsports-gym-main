package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.DTO.MenuSolapasDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Configuration;
import com.blue.open.Code.Repository.ConfigurationRepository;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Guarda y lee las solapas del menu administrativo que un club decidio ocultar.
 *
 * Persiste en la tabla configurations (entidad Configuration) con la llave
 * CONFIG_SOLAPAS_OCULTAS, una fila por club, con el valor serializado como arreglo JSON.
 * Siempre opera sobre el club actual (header X-Company-Uuid), nunca sobre otro: por eso no
 * reutiliza ConfiguracionService.saveOrCreate, que acepta cualquier companyUuid.
 */
@Service
@Slf4j
public class MenuSolapasService {

    // * Llave de la fila en configurations; privada (global = false) y por club
    static final String CONFIG_SOLAPAS_OCULTAS = "menu.solapas.ocultas";

    @Autowired
    private ConfigurationRepository configurationRepository;

    @Autowired
    private AuthService authService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Obtiene las solapas ocultas del club actual.
     *
     * Depende de: AuthService para resolver el club del header X-Company-Uuid.
     * Espera: nada.
     * Responde: MenuSolapasDTO con la lista (vacia si el club nunca configuro el menu o si el
     * valor guardado no es un JSON valido).
     */
    @Transactional(readOnly = true)
    public MenuSolapasDTO obtener() {
        Company company = this.authService.getCurrentCompanyOrThrow();
        Configuration configuracion = this.configurationRepository
                .findFirstByNameAndCompany(CONFIG_SOLAPAS_OCULTAS, company);

        MenuSolapasDTO dto = new MenuSolapasDTO();
        if (configuracion == null || configuracion.getValue() == null || configuracion.getValue().isBlank()) {
            return dto;
        }
        try {
            dto.setSolapasOcultas(this.objectMapper.readValue(
                    configuracion.getValue(), new TypeReference<List<String>>() { }));
        } catch (JsonProcessingException e) {
            // ! Un valor corrupto no debe dejar al club sin menu: se muestra todo
            log.warn("MenuSolapasService | obtener | valor invalido | companyId={}", company.getId(), e);
        }
        return dto;
    }

    /**
     * Reemplaza las solapas ocultas del club actual.
     *
     * Depende de: AuthService para resolver el club del header X-Company-Uuid.
     * Espera: MenuSolapasDTO ya validado; los identificadores repetidos se guardan una vez.
     * Responde: MenuSolapasDTO con lo que quedo guardado.
     */
    @Transactional
    public MenuSolapasDTO guardar(MenuSolapasDTO request) {
        Company company = this.authService.getCurrentCompanyOrThrow();
        List<String> solapasOcultas = new ArrayList<>(new LinkedHashSet<>(request.getSolapasOcultas()));

        Configuration configuracion = this.configurationRepository
                .findFirstByNameAndCompany(CONFIG_SOLAPAS_OCULTAS, company);
        if (configuracion == null) {
            configuracion = new Configuration();
            configuracion.setName(CONFIG_SOLAPAS_OCULTAS);
            configuracion.setCompany(company);
            configuracion.setGlobal(false);
        }
        try {
            configuracion.setValue(this.objectMapper.writeValueAsString(solapasOcultas));
        } catch (JsonProcessingException e) {
            throw new CustomException("No se pudo guardar la configuración del menú");
        }
        this.configurationRepository.save(configuracion);

        MenuSolapasDTO respuesta = new MenuSolapasDTO();
        respuesta.setSolapasOcultas(solapasOcultas);
        return respuesta;
    }
}
