package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.Enums.PeriodicidadCuotaEnum;
import com.blue.open.Code.Entity.DTO.ClubAdminRequestDTO;
import com.blue.open.Code.Entity.DTO.ClubProfileFieldsDTO;
import com.blue.open.Code.Entity.DTO.ClubProfileUpdateRequestDTO;
import com.blue.open.Code.Entity.DTO.ClubRegistrationRequestDTO;
import com.blue.open.Code.Entity.DTO.CompanyDireccionDTO;
import com.blue.open.Code.Entity.Db.CompanyDireccion;

public final class ClubProfileTestData {

    private ClubProfileTestData() {
    }

    public static CompanyDireccionDTO sampleDireccion() {
        CompanyDireccionDTO direccion = new CompanyDireccionDTO();
        direccion.setDireccion("Av. Libertador 1200");
        direccion.setCodigoPostal("1425");
        direccion.setLocalidad("CABA");
        return direccion;
    }

    public static CompanyDireccionDTO sampleDireccionFacturacion() {
        CompanyDireccionDTO direccion = new CompanyDireccionDTO();
        direccion.setDireccion("Av. Corrientes 500");
        direccion.setCodigoPostal("1043");
        direccion.setLocalidad("CABA");
        return direccion;
    }

    public static CompanyDireccionDTO sampleDireccionEntregaFacturas() {
        CompanyDireccionDTO direccion = new CompanyDireccionDTO();
        direccion.setDireccion("Av. Santa Fe 2000");
        direccion.setCodigoPostal("1123");
        direccion.setLocalidad("CABA");
        return direccion;
    }

    public static CompanyDireccion sampleDireccionEntity() {
        CompanyDireccion direccion = new CompanyDireccion();
        direccion.setDireccion("Av. Siempre Viva 742");
        direccion.setCodigoPostal("1425");
        direccion.setLocalidad("Springfield");
        return direccion;
    }

    public static CompanyDireccion toDireccionEntity(CompanyDireccionDTO dto) {
        CompanyDireccion direccion = new CompanyDireccion();
        direccion.setDireccion(dto.getDireccion());
        direccion.setCodigoPostal(dto.getCodigoPostal());
        direccion.setLocalidad(dto.getLocalidad());
        return direccion;
    }

    public static CompanyDireccion sampleDireccionFacturacionEntity() {
        return toDireccionEntity(sampleDireccionFacturacion());
    }

    public static CompanyDireccion sampleDireccionEntregaFacturasEntity() {
        return toDireccionEntity(sampleDireccionEntregaFacturas());
    }

    public static void fillOpenDataFields(ClubProfileFieldsDTO request) {
        request.setPath("club-atletico-norte");
        request.setDomicilio(sampleDireccion());
        request.setDomicilioFacturacion(sampleDireccionFacturacion());
        request.setEmailFacturaElectronica("facturas@catnorte.com");
    }

    public static ClubProfileUpdateRequestDTO validUpdateRequest() {
        ClubProfileUpdateRequestDTO request = new ClubProfileUpdateRequestDTO();
        request.setNombreComercial("Club Atlético Norte");
        request.setRazonSocial("Club Atlético Norte S.A.");
        request.setCuit(CuitValidatorServiceTest.VALID_CUIT_FOR_TESTS);
        request.setTelefono("1140001000");
        request.setEmailInstitucional("info@catnorte.com");
        request.setColorPrimario("#003366");
        request.setColorSecundario("#FFCC00");
        request.setCuotaSocialHabilitada(true);
        request.setPeriodicidadCuotaSocial(PeriodicidadCuotaEnum.BIMESTRAL);
        request.setDiasVencimientoMoroso(35);
        request.setDiasInhabilitacion(95);
        fillOpenDataFields(request);
        return request;
    }

    public static ClubRegistrationRequestDTO validRegistrationRequest() {
        ClubRegistrationRequestDTO request = new ClubRegistrationRequestDTO();
        request.setNombreComercial("Club Atlético Norte");
        request.setRazonSocial("Club Atlético Norte S.A.");
        request.setCuit(CuitValidatorServiceTest.VALID_CUIT_FOR_TESTS);
        request.setTelefono("1140001000");
        request.setEmailInstitucional("info@catnorte.com");
        request.setColorPrimario("#003366");
        request.setColorSecundario("#FFCC00");
        request.setCuotaSocialHabilitada(true);
        request.setPeriodicidadCuotaSocial(PeriodicidadCuotaEnum.BIMESTRAL);
        request.setDiasVencimientoMoroso(35);
        request.setDiasInhabilitacion(95);
        fillOpenDataFields(request);

        ClubAdminRequestDTO admin = new ClubAdminRequestDTO();
        admin.setNombre("María");
        admin.setApellido("González");
        admin.setEmail("admin@catnorte.com");
        admin.setPassword("ClaveSegura123!");
        request.setAdmin(admin);
        return request;
    }
}
