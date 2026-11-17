package com.blue.open.Code.Service.Admin;

import com.blue.open.Code.Entity.DTO.AdminAltaCommand;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Service.ClubRoleProvisioningService;
import com.blue.open.Code.Service.MemberUserProvisioningService;
import com.blue.open.Code.Service.support.RecordingAdminCupoService;
import com.blue.open.Code.Service.support.StubAuthService;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Repository.UserCompanyRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * ClubRoleProvisioningService y MemberUserProvisioningService no se mockean con @Mock: son
 * clases concretas y bytebuddy no puede instrumentarlas bajo JDK 25 en este entorno (mismo
 * problema documentado para CrmIntegracionService). No hace falta un doble funcional para ellas
 * en estos tests: el cupo agotado corta antes de que se las llegue a invocar, así que alcanza con
 * dejarlas sin wire (null) y verificar que nunca se tocan otras dependencias mockeables.
 */
@ExtendWith(MockitoExtension.class)
class AdminAltaServiceTest {

    @Mock
    private UserCompanyRoleRepository userCompanyRoleRepository;

    private AdminAltaService service;
    private RecordingAdminCupoService adminCupoService;
    private StubAuthService stubAuthService;
    private Company company;

    @BeforeEach
    void setUp() {
        service = new AdminAltaService();
        adminCupoService = new RecordingAdminCupoService();
        stubAuthService = new StubAuthService();

        company = new Company();
        company.setId(10L);
        company.setUuid("club-uuid");
        stubAuthService.setCompany(company);

        ReflectionTestUtils.setField(service, "authService", stubAuthService);
        ReflectionTestUtils.setField(service, "adminCupoService", adminCupoService);
        ReflectionTestUtils.setField(service, "clubRoleProvisioningService", new ClubRoleProvisioningService());
        ReflectionTestUtils.setField(service, "userProvisioningService", new MemberUserProvisioningService());
        ReflectionTestUtils.setField(service, "userCompanyRoleRepository", userCompanyRoleRepository);
    }

    @Test
    void alta_sinCupoDisponible_noCreaNada() {
        adminCupoService.setExceptionToThrow(new CustomException(HttpStatus.BAD_REQUEST,
                "El club alcanzó el límite de administradores de su plan", "test"));

        AdminAltaCommand command = altaCommand();

        CustomException ex = assertThrows(CustomException.class, () -> service.alta(command));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals(company, adminCupoService.getLastCompany());
        assertEquals(1, adminCupoService.getLastNuevosAdmins());
        verifyNoInteractions(userCompanyRoleRepository);
    }

    private AdminAltaCommand altaCommand() {
        AdminAltaCommand command = new AdminAltaCommand();
        command.setNombre("Nuevo");
        command.setApellido("Admin");
        command.setEmail("nuevo.admin@test.com");
        command.setPassword("secret123");
        return command;
    }
}
