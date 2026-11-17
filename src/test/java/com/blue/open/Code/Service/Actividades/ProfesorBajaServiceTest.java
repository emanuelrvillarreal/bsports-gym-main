package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Profesor;
import com.blue.open.Code.Entity.Enums.ProfesorEstadoEnum;
import com.blue.open.Code.Repository.ActividadProfesorRepository;
import com.blue.open.Code.Repository.ProfesorRepository;
import com.blue.open.Code.Service.support.StubAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfesorBajaServiceTest {

    @Mock
    private ProfesorRepository profesorRepository;

    @Mock
    private ActividadProfesorRepository actividadProfesorRepository;

    private StubAuthService stubAuthService;
    private ProfesorBajaService service;

    @BeforeEach
    void setUp() {
        stubAuthService = new StubAuthService();
        service = new ProfesorBajaService();
        ReflectionTestUtils.setField(service, "authService", stubAuthService);
        ReflectionTestUtils.setField(service, "profesorRepository", profesorRepository);
        ReflectionTestUtils.setField(service, "actividadProfesorRepository", actividadProfesorRepository);
    }

    @Test
    void bajaLogica_desasignaActividadesYMarcaEliminado() {
        Company company = new Company();
        company.setId(1L);
        stubAuthService.setCompany(company);

        Profesor profesor = new Profesor();
        profesor.setId(5L);
        profesor.setUuid("prof-uuid");
        profesor.setStatus(0);
        profesor.setEstado(ProfesorEstadoEnum.ACTIVO);

        when(profesorRepository.findByUuidAndCompanyId("prof-uuid", 1L)).thenReturn(Optional.of(profesor));

        service.bajaLogica("prof-uuid");

        verify(actividadProfesorRepository).bajaLogicaByProfesorId(5L);
        assertEquals(-1, profesor.getStatus());
        assertEquals(ProfesorEstadoEnum.INACTIVO, profesor.getEstado());
        verify(profesorRepository).save(profesor);
    }

    @Test
    void bajaLogica_idempotenteSiYaEliminado() {
        Company company = new Company();
        company.setId(1L);
        stubAuthService.setCompany(company);

        Profesor profesor = new Profesor();
        profesor.setId(5L);
        profesor.setStatus(-1);

        when(profesorRepository.findByUuidAndCompanyId("prof-uuid", 1L)).thenReturn(Optional.of(profesor));

        service.bajaLogica("prof-uuid");

        verify(actividadProfesorRepository, org.mockito.Mockito.never()).bajaLogicaByProfesorId(org.mockito.ArgumentMatchers.anyLong());
        verify(profesorRepository, org.mockito.Mockito.never()).save(profesor);
    }
}
