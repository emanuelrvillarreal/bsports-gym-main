package com.blue.open.Code.Mapper;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.CompanyDireccion;
import com.blue.open.Code.Entity.Db.CompanySuscripcionCrm;
import com.blue.open.Code.Entity.DTO.ClubProfileFieldsDTO;
import com.blue.open.Code.Entity.DTO.CompanyDireccionDTO;
import com.blue.open.Code.Entity.DTO.CompanyDTO;
import com.blue.open.Code.Entity.DTO.CompanyPublicDTO;
import com.blue.open.Code.Entity.DTO.CompanySuscripcionCrmDTO;
import com.blue.open.Core.Mapper.BaseCompanyMapper;
import org.springframework.stereotype.Service;

@Service
public class CompanyMapper extends BaseCompanyMapper<CompanyDTO> {

    @Override
    protected CompanyDTO createDto() {
        return new CompanyDTO();
    }

    @Override
    protected Company createModel() {
        return new Company();
    }

    @Override
    public CompanyDTO modelToDto(Company model) {
        CompanyDTO dto = super.modelToDto(model);
        dto.setCuit(model.getCuit());
        dto.setNombreComercial(model.getNombreComercial());
        dto.setRazonSocial(model.getRazonSocial());
        dto.setDomicilio(direccionToDto(model.getDomicilio()));
        dto.setDomicilioFacturacion(direccionToDto(model.getDomicilioFacturacion()));
        dto.setTelefono(model.getTelefono());
        dto.setEmailFacturaElectronica(model.getEmailFacturaElectronica());
        dto.setEmailInstitucional(model.getEmailInstitucional());
        dto.setLogoUrl(model.getLogoUrl());
        dto.setColorPrimario(model.getColorPrimario());
        dto.setColorSecundario(model.getColorSecundario());
        dto.setCuotaSocialHabilitada(model.getCuotaSocialHabilitada());
        dto.setPeriodicidadCuotaSocial(model.getPeriodicidadCuotaSocial());
        dto.setDiasVencimientoCuota(model.getDiasVencimientoCuota());
        dto.setDiasVencimientoMoroso(model.getDiasVencimientoMoroso());
        dto.setDiasInhabilitacion(model.getDiasInhabilitacion());
        dto.setAdminCreado(model.getAdminCreado());
        dto.setCertificadoVigenciaModo(model.getCertificadoVigenciaModo());
        dto.setCertificadoVigenciaMeses(model.getCertificadoVigenciaMeses());
        dto.setCertificadoRenovacionDia(model.getCertificadoRenovacionDia());
        dto.setCertificadoRenovacionMes(model.getCertificadoRenovacionMes());
        dto.setRecargoHabilitado(model.getRecargo().getHabilitado());
        dto.setRecargoTipo(model.getRecargo().getTipo());
        dto.setRecargoValor(model.getRecargo().getValor());
        dto.setRecargoDiasGracia(model.getRecargo().getDiasGracia());
        if (dto.getNombreComercial() == null || dto.getNombreComercial().isBlank()) {
            dto.setNombreComercial(model.getName());
        }
        return dto;
    }

    @Override
    public Company dtoToModel(CompanyDTO dto, Company model) {
        model = super.dtoToModel(dto, model);
        model.setCuit(dto.getCuit());
        model.setNombreComercial(dto.getNombreComercial());
        model.setRazonSocial(dto.getRazonSocial());
        model.setDomicilio(direccionToModel(dto.getDomicilio()));
        model.setDomicilioFacturacion(direccionToModel(dto.getDomicilioFacturacion()));
        model.setTelefono(dto.getTelefono());
        model.setEmailFacturaElectronica(dto.getEmailFacturaElectronica());
        model.setEmailInstitucional(dto.getEmailInstitucional());
        model.setLogoUrl(dto.getLogoUrl());
        model.setColorPrimario(dto.getColorPrimario());
        model.setColorSecundario(dto.getColorSecundario());
        model.setCuotaSocialHabilitada(dto.getCuotaSocialHabilitada());
        if (dto.getPeriodicidadCuotaSocial() != null) {
            model.setPeriodicidadCuotaSocial(dto.getPeriodicidadCuotaSocial());
        }
        if (dto.getDiasVencimientoCuota() != null) {
            model.setDiasVencimientoCuota(dto.getDiasVencimientoCuota());
        }
        if (dto.getDiasVencimientoMoroso() != null) {
            model.setDiasVencimientoMoroso(dto.getDiasVencimientoMoroso());
        }
        if (dto.getDiasInhabilitacion() != null) {
            model.setDiasInhabilitacion(dto.getDiasInhabilitacion());
        }
        model.setAdminCreado(dto.getAdminCreado());
        model.configurarCertificado(dto.getCertificadoVigenciaModo(), dto.getCertificadoVigenciaMeses(),
                dto.getCertificadoRenovacionDia(), dto.getCertificadoRenovacionMes());
        model.getRecargo().configurar(dto.getRecargoHabilitado(), dto.getRecargoTipo(), dto.getRecargoValor(),
                dto.getRecargoDiasGracia(), java.time.LocalDate.now());
        syncCommercialName(model, dto);
        return model;
    }

    /**
     * Traduce la suscripcion del club al plan del CRM. No expone uuidFacturaInicialCrm: es un
     * dato de auditoria del alta, no aporta nada a una vista de listado/paginado.
     * <p>
     * Espera que csc.getPlan() ya este inicializado (JOIN FETCH), no lo carga por su cuenta.
     */
    public CompanySuscripcionCrmDTO suscripcionCrmToDto(CompanySuscripcionCrm model) {
        if (model == null) {
            return null;
        }
        CompanySuscripcionCrmDTO dto = new CompanySuscripcionCrmDTO();
        if (model.getPlan() != null) {
            dto.setCodigoPlan(model.getPlan().getCodigo());
            dto.setNombrePlan(model.getPlan().getNombre());
        }
        dto.setUuidSuscripcionCrm(model.getUuidSuscripcionCrm());
        dto.setEstado(model.getEstado());
        if (model.getFechaProximaFacturacion() != null) {
            dto.setFechaProximaFacturacion(model.getFechaProximaFacturacion().toLocalDate());
        }
        return dto;
    }

    public CompanyPublicDTO modelToPublicDto(Company model) {
        CompanyPublicDTO dto = new CompanyPublicDTO();
        dto.setUuid(model.getUuid());
        dto.setName(model.getName());
        dto.setRazonSocial(model.getRazonSocial());
        dto.setNombreComercial(model.getNombreComercial());
        dto.setLogoUrl(model.getLogoUrl());
        dto.setColorPrimario(model.getColorPrimario());
        dto.setColorSecundario(model.getColorSecundario());
        return dto;
    }

    public CompanyDTO profileFieldsToDto(ClubProfileFieldsDTO request) {
        CompanyDTO companyDTO = new CompanyDTO();
        companyDTO.setNombreComercial(request.getNombreComercial().trim());
        companyDTO.setRazonSocial(request.getRazonSocial().trim());
        // El CUIT se guarda normalizado a solo digitos: es la forma en la que lo informa el CRM
        // en sus webhooks, y asi la busqueda del club por CUIT es una igualdad directa
        companyDTO.setCuit(request.getCuit().replaceAll("[^0-9]", ""));
        companyDTO.setDomicilio(trimDireccion(request.getDomicilio()));
        companyDTO.setDomicilioFacturacion(trimDireccion(request.getDomicilioFacturacion()));
        companyDTO.setTelefono(request.getTelefono().trim());
        companyDTO.setEmailFacturaElectronica(request.getEmailFacturaElectronica().trim());
        companyDTO.setEmailInstitucional(request.getEmailInstitucional().trim());
        companyDTO.setPath(request.getPath().trim());
        companyDTO.setColorPrimario(request.getColorPrimario().trim());
        companyDTO.setColorSecundario(request.getColorSecundario().trim());
        companyDTO.setCuotaSocialHabilitada(request.getCuotaSocialHabilitada());
        companyDTO.setPeriodicidadCuotaSocial(request.getPeriodicidadCuotaSocial());
        companyDTO.setDiasVencimientoCuota(request.getDiasVencimientoCuota());
        companyDTO.setDiasVencimientoMoroso(request.getDiasVencimientoMoroso());
        companyDTO.setDiasInhabilitacion(request.getDiasInhabilitacion());
        companyDTO.setCertificadoVigenciaModo(request.getCertificadoVigenciaModo());
        companyDTO.setCertificadoVigenciaMeses(request.getCertificadoVigenciaMeses());
        companyDTO.setCertificadoRenovacionDia(request.getCertificadoRenovacionDia());
        companyDTO.setCertificadoRenovacionMes(request.getCertificadoRenovacionMes());
        companyDTO.setRecargoHabilitado(request.getRecargoHabilitado());
        companyDTO.setRecargoTipo(request.getRecargoTipo());
        companyDTO.setRecargoValor(request.getRecargoValor());
        companyDTO.setRecargoDiasGracia(request.getRecargoDiasGracia());
        return companyDTO;
    }

    private CompanyDireccionDTO trimDireccion(CompanyDireccionDTO direccion) {
        CompanyDireccionDTO trimmed = new CompanyDireccionDTO();
        trimmed.setDireccion(direccion.getDireccion().trim());
        trimmed.setCodigoPostal(direccion.getCodigoPostal().trim());
        trimmed.setLocalidad(direccion.getLocalidad().trim());
        return trimmed;
    }

    private CompanyDireccionDTO direccionToDto(CompanyDireccion direccion) {
        if (direccion == null) {
            return null;
        }
        CompanyDireccionDTO dto = new CompanyDireccionDTO();
        dto.setDireccion(direccion.getDireccion());
        dto.setCodigoPostal(direccion.getCodigoPostal());
        dto.setLocalidad(direccion.getLocalidad());
        return dto;
    }

    private CompanyDireccion direccionToModel(CompanyDireccionDTO dto) {
        if (dto == null) {
            return null;
        }
        CompanyDireccion direccion = new CompanyDireccion();
        direccion.setDireccion(dto.getDireccion());
        direccion.setCodigoPostal(dto.getCodigoPostal());
        direccion.setLocalidad(dto.getLocalidad());
        return direccion;
    }

    private void syncCommercialName(Company model, CompanyDTO dto) {
        String nombreComercial = dto.getNombreComercial();
        if (nombreComercial != null && !nombreComercial.isBlank()) {
            model.setNombreComercial(nombreComercial.trim());
            model.setName(nombreComercial.trim());
            return;
        }
        if (dto.getName() != null && !dto.getName().isBlank()) {
            model.setName(dto.getName().trim());
            if (model.getNombreComercial() == null || model.getNombreComercial().isBlank()) {
                model.setNombreComercial(dto.getName().trim());
            }
        }
    }
}
