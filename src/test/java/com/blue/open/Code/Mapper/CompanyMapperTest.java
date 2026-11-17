package com.blue.open.Code.Mapper;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.DTO.CompanyDTO;
import com.blue.open.Code.Service.ClubProfileTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanyMapperTest {

    private CompanyMapper companyMapper;

    @BeforeEach
    void setUp() {
        companyMapper = new CompanyMapper();
    }

    @Test
    void modelToDto_and_dtoToModel_roundTripClubFields() {
        Company model = new Company();
        model.setUuid("club-uuid-1");
        model.setNombreComercial("Club Atlético");
        model.setRazonSocial("Club Atlético S.A.");
        model.setCuit("30123456789");
        model.setDomicilio(ClubProfileTestData.sampleDireccionEntity());
        model.setDomicilioFacturacion(ClubProfileTestData.sampleDireccionFacturacionEntity());
        model.setTelefono("1140001000");
        model.setEmailFacturaElectronica("facturas@club.com");
        model.setEmailInstitucional("info@club.com");
        model.setLogoUrl("https://cdn.example/logo.png");
        model.setColorPrimario("#003366");
        model.setColorSecundario("#FFCC00");
        model.setCuotaSocialHabilitada(true);
        model.setAdminCreado(true);
        model.setName("Club Atlético");

        CompanyDTO dto = companyMapper.modelToDto(model);

        assertEquals("Club Atlético", dto.getNombreComercial());
        assertEquals("30123456789", dto.getCuit());
        assertEquals("Av. Siempre Viva 742", dto.getDomicilio().getDireccion());
        assertEquals("1425", dto.getDomicilio().getCodigoPostal());
        assertEquals("https://cdn.example/logo.png", dto.getLogoUrl());
        assertTrue(dto.getCuotaSocialHabilitada());

        dto.setNombreComercial("Club Renovado");
        dto.setLogoUrl("https://cdn.example/logo-v2.png");

        Company mapped = companyMapper.dtoToModel(dto, new Company());

        assertEquals("Club Renovado", mapped.getNombreComercial());
        assertEquals("Club Renovado", mapped.getName());
        assertEquals("https://cdn.example/logo-v2.png", mapped.getLogoUrl());
        assertNotNull(mapped.getDomicilio());
        assertEquals("Av. Siempre Viva 742", mapped.getDomicilio().getDireccion());
    }
}
