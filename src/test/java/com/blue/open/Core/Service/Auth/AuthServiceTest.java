package com.blue.open.Core.Service.Auth;

import com.blue.open.Core.Auth.BasePermissionEnum;
import com.blue.open.Core.Auth.BaseRoleEnum;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Core.Entity.Db.Permission;
import com.blue.open.Core.Entity.Db.Role;
import com.blue.open.Core.Entity.Db.UserCompanyRole;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Repository.BaseCompanyRepository;
import com.blue.open.Core.Repository.UserRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private AuthBasicService authBasicService;

    @Mock
    private AuthJwtService authJwtService;

    @Mock
    private AuthTokenService authTokenService;

    @Mock
    private BaseCompanyRepository companyRepository;

    @Mock
    private UserRepository userRepository;

    private User sysAdminUser;
    private User normalUser;
    private Company systemCompany;
    private Company clientCompany;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        // Setup companies
        systemCompany = new Company();
        systemCompany.setId(1L);
        systemCompany.setUuid("system-company-uuid");

        clientCompany = new Company();
        clientCompany.setId(2L);
        clientCompany.setUuid("client-company-uuid");

        // Setup SysAdmin User (ID_COMPANY = 1)
        sysAdminUser = new User();
        sysAdminUser.setUuid("sysadmin-uuid");
        UserCompanyRole sysAdminUcr = new UserCompanyRole();
        sysAdminUcr.setCompany(systemCompany);
        Role sysAdminRole = new Role();
        sysAdminRole.setName(BaseRoleEnum.SYSADMIN.name());
        sysAdminUcr.setRoles(List.of(sysAdminRole));
        sysAdminUser.setUserCompanyRoles(new ArrayList<>(List.of(sysAdminUcr)));

        // Setup Normal User (ID_COMPANY = 2)
        normalUser = new User();
        normalUser.setUuid("normal-user-uuid");
        UserCompanyRole clientUcr = new UserCompanyRole();
        clientUcr.setCompany(clientCompany);
        Role clientRole = new Role();
        clientRole.setName("USER"); // Custom role
        clientUcr.setRoles(List.of(clientRole));
        normalUser.setUserCompanyRoles(new ArrayList<>(List.of(clientUcr)));

        lenient().when(userRepository.findFirstByUuidWithDetails(anyString())).thenAnswer(invocation -> {
            String uuid = invocation.getArgument(0);
            if (sysAdminUser.getUuid().equals(uuid)) {
                return sysAdminUser;
            }
            if (normalUser.getUuid().equals(uuid)) {
                return normalUser;
            }
            return null;
        });

        // Default protocols configuration
        ReflectionTestUtils.setField(authService, "protocols", "basic,jwt,tokens");
        ReflectionTestUtils.setField(authService, "multiCompanyEnabled", true);

        // Setup Mock Request
        request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void getActualOrThrow_NoUser_ThrowsException() {
        when(authJwtService.getUser()).thenReturn(null);
        when(authBasicService.getUser()).thenReturn(null);
        when(authTokenService.getUser()).thenReturn(null);

        CustomException ex = assertThrows(CustomException.class, () -> authService.getActualOrThrow());
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getUserMessage().contains("Usuario no identificado"));
    }

    @Test
    void getActualOrThrow_UserFoundInJwt_ReturnsUser() {
        when(authJwtService.getUser()).thenReturn(sysAdminUser);

        User result = authService.getActualOrThrow();
        assertEquals(sysAdminUser.getUuid(), result.getUuid());
    }

    @Test
    void valid_SysAdminInAnyCompany_Success() {
        when(authJwtService.getUser()).thenReturn(sysAdminUser);
        request.addHeader("X-Company-Uuid", clientCompany.getUuid());

        // SysAdmin should be valid even in client company because they belong to
        // Company 1
        assertDoesNotThrow(() -> authService.valid());
    }

    @Test
    void valid_NormalUserInOwnCompany_Success() {
        when(authJwtService.getUser()).thenReturn(normalUser);
        request.addHeader("X-Company-Uuid", clientCompany.getUuid());

        assertDoesNotThrow(() -> authService.valid());
    }

    @Test
    void valid_NormalUserInOtherCompany_ThrowsException() {
        when(authJwtService.getUser()).thenReturn(normalUser);
        request.addHeader("X-Company-Uuid", "other-company-uuid");

        CustomException ex = assertThrows(CustomException.class, () -> authService.valid());
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("Empresa no permitida", ex.getUserMessage());
    }

    @Test
    void validWithRequired_UserHasPermission_Success() {
        // Setup user with specific permission
        Permission viewPermission = new Permission();
        viewPermission.setName(BasePermissionEnum.USER_VIEW.getName());

        Role role = normalUser.getUserCompanyRoles().get(0).getRoles().get(0);
        role.setPermissions(List.of(viewPermission));

        when(authJwtService.getUser()).thenReturn(normalUser);
        request.addHeader("X-Company-Uuid", clientCompany.getUuid());

        assertDoesNotThrow(() -> authService.valid(List.of(BasePermissionEnum.USER_VIEW)));
    }

    @Test
    void validWithRequired_UserHasRole_Success() {
        when(authJwtService.getUser()).thenReturn(sysAdminUser);
        request.addHeader("X-Company-Uuid", systemCompany.getUuid());

        // Validating by BaseRoleEnum
        assertDoesNotThrow(() -> authService.valid(List.of(BaseRoleEnum.SYSADMIN)));
    }

    @Test
    void validWithRequired_UserDoesNotHaveExpectedRole_ThrowsException() {
        when(authJwtService.getUser()).thenReturn(normalUser);
        request.addHeader("X-Company-Uuid", clientCompany.getUuid());

        CustomException ex = assertThrows(CustomException.class, () -> authService.valid(List.of(BaseRoleEnum.ADMIN)));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("No tienes permiso", ex.getUserMessage());
    }

    @Test
    void getCurrentCompanyOrThrow_NoHeader_ThrowsException() {
        when(authJwtService.getUser()).thenReturn(normalUser);
        // No header added to request

        CustomException ex = assertThrows(CustomException.class, () -> authService.getCurrentCompanyOrThrow());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getDeveloperMessage().contains("Falta el header X-Company-Uuid"));
    }

    @Test
    void getCurrentCompanyOrThrow_ValidHeader_ReturnsCompany() {
        when(authJwtService.getUser()).thenReturn(normalUser);
        request.addHeader("X-Company-Uuid", clientCompany.getUuid());

        Company result = authService.getCurrentCompanyOrThrow();
        assertEquals(clientCompany.getUuid(), result.getUuid());
    }

    @Test
    void isSysAdmin_WhenUserInCompany1_ReturnsTrue() {
        assertTrue(authService.isSysAdmin(sysAdminUser));
    }

    @Test
    void isSysAdmin_WhenUserNotInCompany1_ReturnsFalse() {
        assertFalse(authService.isSysAdmin(normalUser));
    }

    @Test
    void isAdmin_WhenUserIsAdminOfSpecifiedCompany_ReturnsTrue() {
        // Build a local admin role
        Role adminRole = new Role();
        adminRole.setName("ADMIN");
        normalUser.getUserCompanyRoles().get(0).setRoles(List.of(adminRole));

        assertTrue(authService.isAdmin(normalUser, clientCompany.getUuid()));
    }

    @Test
    void isAdmin_WhenUserIsGlobalAdmin_ReturnsTrueForAnyCompany() {
        assertTrue(authService.isAdmin(sysAdminUser, "any-company-uuid"));
    }

    @Test
    void getAggregatedPermissions_IncludesSystemPermissionsForGlobalAdmin() {
        Permission p = new Permission();
        p.setName("system.permission");
        sysAdminUser.getUserCompanyRoles().get(0).getRoles().get(0).setPermissions(List.of(p));

        Set<String> perms = authService.getAggregatedPermissions(sysAdminUser, "other-company");
        assertTrue(perms.contains("system.permission"));
    }

    @Test
    void getCurrentCompanyOrThrow_GlobalAdminAccessingNonLinkedCompany_ReturnsCompany() {
        when(authJwtService.getUser()).thenReturn(sysAdminUser);
        String thirdCompanyUuid = "third-company-uuid";
        request.addHeader("X-Company-Uuid", thirdCompanyUuid);

        Company thirdCompany = new Company();
        thirdCompany.setUuid(thirdCompanyUuid);
        when(companyRepository.findFirstByUuidWithStatusIsActive(thirdCompanyUuid)).thenReturn(thirdCompany);

        Company result = authService.getCurrentCompanyOrThrow();
        assertEquals(thirdCompanyUuid, result.getUuid());
        verify(companyRepository).findFirstByUuidWithStatusIsActive(thirdCompanyUuid);
    }
}
