package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.AsistenciaUmbralesDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Configuration;
import com.blue.open.Code.Repository.ConfigurationRepository;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Umbrales del semaforo de asistencia y de las alertas, uno por club.
 *
 * Persiste en configurations (llave {@value #CONFIG_UMBRALES}) como JSON, igual que las solapas del
 * menu. Sin configuracion se usan los valores por defecto de {@link AsistenciaUmbralesDTO}.
 */
@Service
@Slf4j
public class AsistenciaUmbralesService {

    static final String CONFIG_UMBRALES = "asistencia.umbrales";

    @Autowired
    private ConfigurationRepository configurationRepository;

    @Autowired
    private AuthService authService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Transactional(readOnly = true)
    public AsistenciaUmbralesDTO obtener(Company company) {
        Configuration configuracion = configurationRepository.findFirstByNameAndCompany(CONFIG_UMBRALES, company);
        if (configuracion == null || configuracion.getValue() == null || configuracion.getValue().isBlank()) {
            return new AsistenciaUmbralesDTO();
        }
        try {
            return objectMapper.readValue(configuracion.getValue(), AsistenciaUmbralesDTO.class);
        } catch (JsonProcessingException e) {
            // ! Un valor corrupto no debe romper las estadisticas: se usan los valores por defecto
            log.warn("AsistenciaUmbralesService | obtener | valor invalido | companyId={}", company.getId(), e);
            return new AsistenciaUmbralesDTO();
        }
    }

    /** Umbrales del club actual. Los lee cualquiera que vea estadisticas (admin o profesor). */
    @Transactional(readOnly = true)
    public AsistenciaUmbralesDTO obtenerActual() {
        return obtener(authService.getCurrentCompanyOrThrow());
    }

    /** Reemplaza los umbrales del club actual. Solo ADMIN. */
    @Transactional
    public AsistenciaUmbralesDTO guardar(AsistenciaUmbralesDTO request) {
        authService.valid(List.of(PermissionEnum.ACTIVITY_ATTENDANCE_STATS));
        if (request.getAmarillo() >= request.getVerde()) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "El umbral amarillo tiene que ser menor que el verde",
                    "AsistenciaUmbralesService | verde: " + request.getVerde() + " amarillo: " + request.getAmarillo());
        }
        Company company = authService.getCurrentCompanyOrThrow();
        Configuration configuracion = configurationRepository.findFirstByNameAndCompany(CONFIG_UMBRALES, company);
        if (configuracion == null) {
            configuracion = new Configuration();
            configuracion.setName(CONFIG_UMBRALES);
            configuracion.setCompany(company);
            configuracion.setGlobal(false);
        }
        try {
            configuracion.setValue(objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            throw new CustomException("No se pudieron guardar los umbrales de asistencia");
        }
        configurationRepository.save(configuracion);
        return request;
    }
}
