package com.blue.open.Code.Service.CuotaSocialRango;

import com.blue.open.Code.Entity.DTO.CuotaSocialRangoDTO;
import com.blue.open.Code.Entity.DTO.CuotaSocialRangoEdicionCommand;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.CuotaSocialRango;
import com.blue.open.Code.Event.CuotaSocialRangoCambiadoEvent;
import com.blue.open.Code.Mapper.CuotaSocialRangoMapper;
import com.blue.open.Code.Repository.CuotaSocialRangoRepository;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CuotaSocialRangoEdicionService {

    @Autowired
    private AuthService authService;

    @Autowired
    private CuotaSocialRangoRepository cuotaSocialRangoRepository;

    @Autowired
    private CuotaSocialRangoMapper cuotaSocialRangoMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Transactional
    public CuotaSocialRangoDTO editar(String cuotaSocialRangoUuid, CuotaSocialRangoEdicionCommand command) {
        Company company = authService.getCurrentCompanyOrThrow();

        CuotaSocialRango rango = cuotaSocialRangoRepository
                .findByUuidAndCompanyIdAndStatusActive(cuotaSocialRangoUuid, company.getId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Rango de cuota social no encontrado",
                        "CuotaSocialRangoEdicionService | uuid: " + cuotaSocialRangoUuid));

        if (Boolean.TRUE.equals(rango.getEsDefault())) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "No se puede modificar el rango por defecto del gimnasio",
                    "CuotaSocialRangoEdicionService | uuid: " + cuotaSocialRangoUuid);
        }

        String nombre = command.getNombre().trim();
        if (cuotaSocialRangoRepository.existsNombreActivoEnCompany(company.getId(), nombre, rango.getId())) {
            throw new CustomException(HttpStatus.CONFLICT, "Ya existe un rango de cuota social con ese nombre",
                    "CuotaSocialRangoEdicionService | nombre duplicado: " + nombre);
        }
        if (cuotaSocialRangoRepository.existsEdadHastaActivoEnCompany(
                company.getId(), command.getEdadHasta(), rango.getId())) {
            throw new CustomException(HttpStatus.CONFLICT, "Ya existe un rango de cuota social con esa edad hasta",
                    "CuotaSocialRangoEdicionService | edadHasta duplicada: " + command.getEdadHasta());
        }

        rango.setNombre(nombre);
        rango.setDescripcion(normalizeDescripcion(command.getDescripcion()));
        rango.setEdadHasta(command.getEdadHasta());
        rango.setImporte(command.getImporte());

        rango = cuotaSocialRangoRepository.save(rango);

        eventPublisher.publishEvent(new CuotaSocialRangoCambiadoEvent(
                company.getId(), rango.getId(), rango.getUuid()));

        return cuotaSocialRangoMapper.modelToDto(rango);
    }

    private String normalizeDescripcion(String descripcion) {
        if (descripcion == null) {
            return null;
        }
        String trimmed = descripcion.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
