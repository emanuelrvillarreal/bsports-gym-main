package com.blue.open.Code.Service.Tesoreria;

import com.blue.open.Code.Entity.DTO.MercadoPagoTrazaRegistro;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.MercadoPagoTraza;
import com.blue.open.Code.Entity.Enums.MercadoPagoTrazaEtapaEnum;
import com.blue.open.Code.Entity.Enums.MercadoPagoTrazaNivelEnum;
import com.blue.open.Code.Repository.MercadoPagoTrazaRepository;
import com.blue.open.Code.Service.Tesoreria.Cobro.MercadoPagoTrazaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MercadoPagoTrazaServiceTest {

    @Mock
    private MercadoPagoTrazaRepository mercadoPagoTrazaRepository;

    private MercadoPagoTrazaService service;

    @BeforeEach
    void setUp() {
        service = new MercadoPagoTrazaService(mercadoPagoTrazaRepository);
        ReflectionTestUtils.setField(service, "self", service);
    }

    @Test
    void registrar_persisteTraza() {
        Company company = new Company();
        company.setId(7L);

        service.registrar(MercadoPagoTrazaRegistro.builder()
                .company(company)
                .etapa(MercadoPagoTrazaEtapaEnum.COBRO_INICIADO)
                .nivel(MercadoPagoTrazaNivelEnum.INFO)
                .externalReference("ext-ref")
                .mensaje("Cobro iniciado")
                .build());

        ArgumentCaptor<MercadoPagoTraza> captor = ArgumentCaptor.forClass(MercadoPagoTraza.class);
        verify(mercadoPagoTrazaRepository).save(captor.capture());

        MercadoPagoTraza saved = captor.getValue();
        assertEquals(company, saved.getCompany());
        assertEquals(MercadoPagoTrazaEtapaEnum.COBRO_INICIADO, saved.getEtapa());
        assertEquals(MercadoPagoTrazaNivelEnum.INFO, saved.getNivel());
        assertEquals("ext-ref", saved.getExternalReference());
        assertEquals("Cobro iniciado", saved.getMensaje());
    }

    @Test
    void registrar_sinCompany_noPersiste() {
        service.registrar(MercadoPagoTrazaRegistro.builder()
                .etapa(MercadoPagoTrazaEtapaEnum.WEBHOOK_RECIBIDO)
                .build());

        verifyNoInteractions(mercadoPagoTrazaRepository);
    }
}
