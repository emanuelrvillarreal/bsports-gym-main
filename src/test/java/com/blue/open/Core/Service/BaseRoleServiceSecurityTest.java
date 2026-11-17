package com.blue.open.Core.Service;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Service.RoleService;
import com.blue.open.Core.Entity.DTO.PermissionDTO;
import com.blue.open.Core.Entity.DTO.RoleDTO;
import com.blue.open.Core.Entity.Db.Role;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import com.blue.open.Core.Repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BaseRoleServiceSecurityTest {

    @InjectMocks
    private RoleService roleService;

    @Mock
    private AuthService authService;

    @Mock
    private RoleRepository roleRepository;

    private User currentUser;
    private Company systemCompany;
    private Company clientCompany;

    @BeforeEach
    void setUp() {
        systemCompany = new Company();
        systemCompany.setId(1L);
        systemCompany.setUuid("1");

        clientCompany = new Company();
        clientCompany.setId(100L);
        clientCompany.setUuid("company-uuid-100");

        currentUser = new User();
        currentUser.setUuid("user-uuid");
        
        // Manual injection to ensure both shadowed/renamed fields are set
        org.springframework.test.util.ReflectionTestUtils.setField(roleService, "roleRepository", roleRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(roleService, "repository", roleRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(roleService, "authService", authService);
    }

    @Test
    void hookSaveAuth_AdminCannotModifySystemRole_ThrowsException() {
        // ARRANGE: Local admin in Company 100
        when(authService.getActualOrThrow()).thenReturn(currentUser);
        when(authService.isSysAdmin(currentUser)).thenReturn(false);
        when(authService.getCurrentCompanyOrThrow()).thenReturn(clientCompany);

        String roleUuid = "system-role-uuid";
        RoleDTO request = new RoleDTO();
        request.setUuid(roleUuid); 
        request.setName("Modified Name");

        // Mock existing role as belonging to Company 1 (System)
        Role systemRole = new Role();
        systemRole.setUuid(roleUuid);
        systemRole.setCompany(systemCompany);
        
        when(roleRepository.findFirstByUuid(roleUuid)).thenReturn(systemRole);

        // ACT & ASSERT
        CustomException ex = assertThrows(CustomException.class, () -> roleService.hookSaveAuth(request));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getDeveloperMessage().contains("gestionar roles de sistema"));
    }

    @Test
    void hookSaveAuth_EscalationDetected_ThrowsException() {
        // ARRANGE: Admin in Company 100
        when(authService.getActualOrThrow()).thenReturn(currentUser);
        when(authService.isSysAdmin(currentUser)).thenReturn(false);
        when(authService.getCurrentCompanyOrThrow()).thenReturn(clientCompany);

        // Caller has 'VIEW' but tries to assign 'DELETE'
        when(authService.getAggregatedPermissions(currentUser, clientCompany.getUuid()))
                .thenReturn(Set.of("VIEW"));
        lenient().when(authService.valid(anyList())).thenReturn(currentUser);

        RoleDTO request = new RoleDTO();
        request.setPermissions(List.of(new PermissionDTO("DELETE")));

        // ACT & ASSERT
        CustomException ex = assertThrows(CustomException.class, () -> roleService.hookSaveAuth(request));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getUserMessage().contains("Escalación detectada"));
    }

    @Test
    void hookSaveAuth_GlobalAdmin_AllowedAnywhere() {
        // ARRANGE: SysAdmin
        when(authService.getActualOrThrow()).thenReturn(currentUser);
        when(authService.isSysAdmin(currentUser)).thenReturn(true);
        when(authService.getCurrentCompanyOrThrow()).thenReturn(clientCompany);

        RoleDTO request = new RoleDTO();
        request.setPermissions(List.of(new PermissionDTO("ANY_PERMISSION")));

        // ACT & ASSERT: Should NOT throw any exception
        assertDoesNotThrow(() -> roleService.hookSaveAuth(request));
    }
}
