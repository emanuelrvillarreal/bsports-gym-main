package com.blue.open.Core.Service;

import com.blue.open.Code.Entity.DTO.UserDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Service.UserService;
import com.blue.open.Core.Entity.DTO.UserCompanyRoleDTO;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BaseUserServiceSecurityTest {

    @InjectMocks
    private UserService userService;

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
    }

    @Test
    void hookSaveAuth_AdminCannotCreateUserInDifferentCompany_ThrowsException() {
        // ARRANGE: Local admin in Company 100
        when(authService.getActualOrThrow()).thenReturn(currentUser);
        when(authService.getCurrentCompanyOrThrow()).thenReturn(clientCompany);
        when(authService.isSysAdmin(currentUser)).thenReturn(false);

        UserDTO request = new UserDTO();
        request.setUuid(null); // Creation
        UserCompanyRoleDTO ucr = new UserCompanyRoleDTO();
        ucr.setCompanyUuid("other-company-uuid");
        request.setUserCompanyRoles(List.of(ucr));

        // ACT & ASSERT
        CustomException ex = assertThrows(CustomException.class, () -> userService.hookSaveAuth(request));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getDeveloperMessage().contains("una empresa distinta a la tuya"));
    }

    @Test
    void hookSaveAuth_AdminCannotAssignSystemRole_ThrowsException() {
        // ARRANGE: Local admin
        when(authService.getActualOrThrow()).thenReturn(currentUser);
        when(authService.getCurrentCompanyOrThrow()).thenReturn(clientCompany);
        when(authService.isSysAdmin(currentUser)).thenReturn(false);

        UserDTO request = new UserDTO();
        UserCompanyRoleDTO ucr = new UserCompanyRoleDTO();
        ucr.setCompanyUuid(clientCompany.getUuid());

        com.blue.open.Core.Entity.DTO.RoleDTO roleDto = new com.blue.open.Core.Entity.DTO.RoleDTO();
        roleDto.setUuid("sys-role-uuid");
        ucr.setRoles(List.of(roleDto));
        request.setUserCompanyRoles(List.of(ucr));

        Role dbRole = new Role();
        dbRole.setCompany(systemCompany); // Role belongs to Company 1
        when(roleRepository.findFirstByUuid("sys-role-uuid")).thenReturn(dbRole);

        // ACT & ASSERT
        CustomException ex = assertThrows(CustomException.class, () -> userService.hookSaveAuth(request));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertTrue(ex.getDeveloperMessage().contains("No tienes permisos para asignar roles de sistema"));
    }
}
