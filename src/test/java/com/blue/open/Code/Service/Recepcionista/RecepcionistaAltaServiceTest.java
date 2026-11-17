package com.blue.open.Code.Service.Recepcionista;

import com.blue.open.Code.Auth.RoleEnum;
import com.blue.open.Code.Entity.DTO.RecepcionistaAltaCommand;
import com.blue.open.Code.Entity.DTO.RecepcionistaDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Recepcionista;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.RecepcionistaEstadoEnum;
import com.blue.open.Code.Mapper.RecepcionistaMapper;
import com.blue.open.Code.Repository.RecepcionistaRepository;
import com.blue.open.Code.Service.support.StubAuthService;
import com.blue.open.Code.Service.support.StubMemberUserProvisioningService;
import com.blue.open.Core.Exceptions.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecepcionistaAltaServiceTest {

    @Mock
    private RecepcionistaRepository recepcionistaRepository;

    private StubAuthService stubAuthService;
    private StubMemberUserProvisioningService stubUserProvisioningService;
    private RecepcionistaMapper recepcionistaMapper;
    private RecepcionistaAltaService service;

    @BeforeEach
    void setUp() {
        stubAuthService = new StubAuthService();
        stubUserProvisioningService = new StubMemberUserProvisioningService();
        recepcionistaMapper = new RecepcionistaMapper();
        service = new RecepcionistaAltaService();
        ReflectionTestUtils.setField(service, "authService", stubAuthService);
        ReflectionTestUtils.setField(service, "userProvisioningService", stubUserProvisioningService);
        ReflectionTestUtils.setField(service, "recepcionistaRepository", recepcionistaRepository);
        ReflectionTestUtils.setField(service, "recepcionistaMapper", recepcionistaMapper);

        Company company = new Company();
        company.setId(1L);
        stubAuthService.setCompany(company);
    }

    @Test
    void alta_creaRecepcionistaConRolAdmRecepcion() {
        RecepcionistaAltaCommand command = buildCommand();

        User user = new User();
        user.setId(99L);
        user.setUuid("user-uuid");
        user.setNombre("María");
        user.setApellido("López");
        user.setEmail("recepcion@ejemplo.com");
        stubUserProvisioningService.setUserToReturn(user);

        when(recepcionistaRepository.findFirstByCompany_IdAndUser_Id(1L, 99L)).thenReturn(Optional.empty());
        when(recepcionistaRepository.save(any(Recepcionista.class))).thenAnswer(inv -> {
            Recepcionista r = inv.getArgument(0);
            r.setId(10L);
            r.setUuid("rec-uuid");
            return r;
        });
        when(recepcionistaRepository.findByIdWithUser(10L)).thenAnswer(inv -> {
            Recepcionista r = new Recepcionista();
            r.setId(10L);
            r.setUuid("rec-uuid");
            r.setEstado(RecepcionistaEstadoEnum.ACTIVO);
            r.setFechaAlta(LocalDate.of(2026, 6, 1));
            r.setUser(user);
            return Optional.of(r);
        });

        RecepcionistaDTO dto = service.alta(command);

        ArgumentCaptor<Recepcionista> captor = ArgumentCaptor.forClass(Recepcionista.class);
        verify(recepcionistaRepository).save(captor.capture());
        Recepcionista saved = captor.getValue();
        assertEquals(RecepcionistaEstadoEnum.ACTIVO, saved.getEstado());
        assertEquals(LocalDate.of(2026, 6, 1), saved.getFechaAlta());
        assertEquals(user, saved.getUser());
        assertEquals("rec-uuid", dto.getRecepcionistaUuid());
        assertEquals("María", dto.getNombre());
        assertEquals(RoleEnum.ADM_RECEPCION, stubUserProvisioningService.getLastRoleEnum());
    }

    @Test
    void alta_usuarioDuplicadoEnClub_lanzaBadRequest() {
        RecepcionistaAltaCommand command = buildCommand();

        User user = new User();
        user.setId(99L);
        stubUserProvisioningService.setUserToReturn(user);

        Recepcionista activo = new Recepcionista();
        activo.setId(10L);
        activo.setStatus(0);
        when(recepcionistaRepository.findFirstByCompany_IdAndUser_Id(1L, 99L)).thenReturn(Optional.of(activo));

        CustomException ex = assertThrows(CustomException.class, () -> service.alta(command));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(recepcionistaRepository, never()).save(any());
    }

    @Test
    void alta_sinPermiso_lanzaForbidden() {
        stubAuthService.setAllowAccess(false);
        RecepcionistaAltaCommand command = buildCommand();

        assertThrows(CustomException.class, () -> service.alta(command));
        assertEquals(0, stubUserProvisioningService.getCreatePortalUserCalls());
    }

    private RecepcionistaAltaCommand buildCommand() {
        RecepcionistaAltaCommand command = new RecepcionistaAltaCommand();
        command.setNombre("María");
        command.setApellido("López");
        command.setEmail("recepcion@ejemplo.com");
        command.setPassword("Cambiar123");
        command.setFechaAlta(LocalDate.of(2026, 6, 1));
        return command;
    }
}
