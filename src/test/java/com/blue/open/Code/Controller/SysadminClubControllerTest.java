package com.blue.open.Code.Controller;

import com.blue.open.Code.Entity.DTO.ClubRegistrationRequestDTO;
import com.blue.open.Code.Entity.DTO.ClubRegistrationResponseDTO;
import com.blue.open.Code.Entity.DTO.CompanyDTO;
import com.blue.open.Code.Entity.DTO.UserDTO;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Service.SysadminClubService;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SysadminClubControllerTest {

    private SysadminClubController controller;
    private StubAuthService stubAuthService;
    private StubSysadminClubService stubSysadminClubService;

    @BeforeEach
    void setUp() {
        controller = new SysadminClubController();
        stubAuthService = new StubAuthService();
        stubSysadminClubService = new StubSysadminClubService();
        ReflectionTestUtils.setField(controller, "authService", stubAuthService);
        ReflectionTestUtils.setField(controller, "sysadminClubService", stubSysadminClubService);
    }

    @Test
    void registerClub_returns403WhenNotSysadmin() {
        stubAuthService.setAllowAccess(false);

        CustomException ex = assertThrows(CustomException.class,
                () -> controller.registerClub(new ClubRegistrationRequestDTO()));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void registerClub_returns201WhenSysadmin() {
        stubAuthService.setAllowAccess(true);

        ClubRegistrationResponseDTO expected = new ClubRegistrationResponseDTO();
        CompanyDTO company = new CompanyDTO();
        company.setUuid("club-uuid");
        company.setNombreComercial("Club Test");
        company.setPath("club-test");
        expected.setCompany(company);
        UserDTO admin = new UserDTO();
        admin.setEmail("admin@test.com");
        expected.setAdmin(admin);
        stubSysadminClubService.setResponse(expected);

        ClubRegistrationRequestDTO request = new ClubRegistrationRequestDTO();
        request.setNombreComercial("Club Test");
        request.setPath("club-test");

        ResponseEntity<ClubRegistrationResponseDTO> response = controller.registerClub(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("club-uuid", response.getBody().getCompany().getUuid());
        assertEquals("club-test", response.getBody().getCompany().getPath());
        assertEquals("admin@test.com", response.getBody().getAdmin().getEmail());
        assertEquals(request, stubSysadminClubService.getLastRequest());
    }

    private static class StubAuthService extends AuthService {

        private boolean allowAccess = true;

        void setAllowAccess(boolean allowAccess) {
            this.allowAccess = allowAccess;
        }

        @Override
        public User valid(List<Enum<?>> required) {
            if (!allowAccess) {
                throw new CustomException(HttpStatus.FORBIDDEN, "No permitido", "SYSADMIN required");
            }
            return new User();
        }
    }

    private static class StubSysadminClubService extends SysadminClubService {

        private ClubRegistrationResponseDTO response;
        private ClubRegistrationRequestDTO lastRequest;

        void setResponse(ClubRegistrationResponseDTO response) {
            this.response = response;
        }

        ClubRegistrationRequestDTO getLastRequest() {
            return lastRequest;
        }

        @Override
        public ClubRegistrationResponseDTO registerClub(ClubRegistrationRequestDTO request) {
            lastRequest = request;
            return response;
        }
    }
}
