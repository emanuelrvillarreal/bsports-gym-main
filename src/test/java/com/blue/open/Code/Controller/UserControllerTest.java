package com.blue.open.Code.Controller;

import com.blue.open.Code.Entity.DTO.UserDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Service.UserService;
import com.blue.open.Core.Entity.DTO.UserCompanyRoleDTO;
import com.blue.open.Core.Entity.DTO.RoleDTO;
import com.blue.open.Core.Entity.Db.Role;
import com.blue.open.Core.Entity.Db.UserCompanyRole;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Repository.RoleRepository;
import com.blue.open.Core.Service.Auth.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserService userServiceMock; 

    @InjectMocks
    private UserService userService; 

    private User currentUser;
    private Company company1; // System Company
    private Company company2; // Client Company

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setUuid("user-uuid-current");
        currentUser.setUserCompanyRoles(new ArrayList<>());

        company1 = new Company();
        company1.setId(1L);
        company1.setUuid("1");

        company2 = new Company();
        company2.setId(2L);
        company2.setUuid("company-uuid-2");
    }

    private void setupAuth(User user, Company headerCompany) {
        lenient().when(authService.getActualOrThrow()).thenReturn(user);
        lenient().when(authService.getCurrentCompanyOrThrow()).thenReturn(headerCompany);
        lenient().when(authService.valid(anyList())).thenReturn(user);
        lenient().when(authService.valid()).thenReturn(user);
        lenient().when(authService.isSysAdmin(any())).thenReturn(false);
        lenient().when(authService.isAdmin(any(), anyString())).thenReturn(false);
    }

    private void setupRoleMock(String uuid, String name, Company company) {
        Role role = new Role();
        role.setUuid(uuid);
        role.setName(name);
        role.setCompany(company);
        role.setPermissions(new ArrayList<>());
        lenient().when(roleRepository.findFirstByUuid(eq(uuid))).thenReturn(role);
        lenient().when(roleRepository.findFirstByUuid(isNull())).thenReturn(null);
    }

    private void buildUserCompanyRole(User user, Company company, String... roleNames) {
        UserCompanyRole ucr = new UserCompanyRole();
        ucr.setUser(user);
        ucr.setCompany(company);
        List<Role> roles = new ArrayList<>();
        for (String roleName : roleNames) {
            Role role = new Role();
            role.setName(roleName);
            roles.add(role);
        }
        ucr.setRoles(roles);
        user.getUserCompanyRoles().add(ucr);
    }

    private UserCompanyRoleDTO createUserCompanyRoleDTO(String companyUuid, String roleUuid, String roleName) {
        UserCompanyRoleDTO ucrDTO = new UserCompanyRoleDTO();
        ucrDTO.setCompanyUuid(companyUuid);

        if (roleName != null || roleUuid != null) {
            RoleDTO roleDTO = new RoleDTO();
            roleDTO.setUuid(roleUuid);
            roleDTO.setName(roleName);
            ucrDTO.setRoles(List.of(roleDTO));
        } else {
            ucrDTO.setRoles(new ArrayList<>());
        }
        return ucrDTO;
    }

    private RoleDTO createRoleDTO(String name, String uuid) {
        RoleDTO roleDTO = new RoleDTO();
        roleDTO.setName(name);
        roleDTO.setUuid(uuid);
        return roleDTO;
    }

    @Test
    void hookSaveAuth_SysAdminCanAssignAnyRole_Success() {
        buildUserCompanyRole(currentUser, company1, "SYSADMIN");
        setupAuth(currentUser, company2);
        lenient().when(authService.isSysAdmin(currentUser)).thenReturn(true);

        UserDTO request = new UserDTO();
        request.setUserCompanyRoles(List.of(createUserCompanyRoleDTO(company2.getUuid(), "admin-role-uuid", "ADMIN")));

        setupRoleMock("admin-role-uuid", "ADMIN", company2);

        assertDoesNotThrow(() -> userService.hookSaveAuth(request));
    }

    @Test
    void hookSaveAuth_AdminCannotAssignSysAdminRole_ThrowsException() {
        buildUserCompanyRole(currentUser, company2, "ADMIN");
        setupAuth(currentUser, company2);

        UserDTO request = new UserDTO();
        request.setUserCompanyRoles(List.of(createUserCompanyRoleDTO(company2.getUuid(), "sys-role-uuid", "SYSADMIN")));

        setupRoleMock("sys-role-uuid", "SYSADMIN", company1);

        CustomException exception = assertThrows(CustomException.class, () -> userService.hookSaveAuth(request));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertTrue(exception.getDeveloperMessage().contains("No tienes permisos para asignar roles de sistema"));
    }

    @Test
    void hookSaveAuth_UserCannotAdministerOtherCompanies_ThrowsException() {
        buildUserCompanyRole(currentUser, company2, "ADMIN");
        setupAuth(currentUser, company2);

        UserDTO request = new UserDTO();
        request.setUserCompanyRoles(List.of(createUserCompanyRoleDTO(company1.getUuid(), "some-role-uuid", "SOME_ROLE")));

        CustomException exception = assertThrows(CustomException.class, () -> userService.hookSaveAuth(request));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertTrue(exception.getDeveloperMessage().contains("No puedes crear/editar usuarios para una empresa distinta a la tuya."));
    }

    @Test
    void hookSaveAuth_SystemStaffCanAdministerOtherCompanies_Success() {
        buildUserCompanyRole(currentUser, company1, "SUPPORT");
        setupAuth(currentUser, company2);

        UserDTO request = new UserDTO();
        request.setUserCompanyRoles(List.of(createUserCompanyRoleDTO(company2.getUuid(), "operator-role-uuid", "OPERATOR")));

        setupRoleMock("operator-role-uuid", "OPERATOR", company2);

        assertDoesNotThrow(() -> userService.hookSaveAuth(request));
    }

    @Test
    void hookSaveAuth_SystemStaffCannotAssignHardcodedAdminRole_ThrowsException() {
        buildUserCompanyRole(currentUser, company1, "SUPPORT");
        setupAuth(currentUser, company2);

        UserDTO request = new UserDTO();
        request.setUserCompanyRoles(List.of(createUserCompanyRoleDTO(company2.getUuid(), "admin-role-uuid", "ADMIN")));

        setupRoleMock("admin-role-uuid", "ADMIN", company2);

        CustomException exception = assertThrows(CustomException.class, () -> userService.hookSaveAuth(request));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertTrue(exception.getDeveloperMessage().contains("No puedes asignar el rol 'ADMIN'"));
    }

    @Test
    void hookSaveAuth_NormalUserWithCreatePermissionCanAssignRoles_Success() {
        buildUserCompanyRole(currentUser, company2, "MANAGER");
        setupAuth(currentUser, company2);

        UserDTO request = new UserDTO();
        request.setUuid(null); 
        request.setUserCompanyRoles(List.of(createUserCompanyRoleDTO(company2.getUuid(), "assistant-role-uuid", "ASSISTANT")));

        setupRoleMock("assistant-role-uuid", "ASSISTANT", company2);

        assertDoesNotThrow(() -> userService.hookSaveAuth(request));
    }

    @Test
    void hookSaveAuth_UserCannotElevateTheirOwnRole_ThrowsException() {
        buildUserCompanyRole(currentUser, company2, "MANAGER");
        setupAuth(currentUser, company2);
        
        UserDTO request = new UserDTO();
        request.setUuid(currentUser.getUuid());
        request.setUserCompanyRoles(List.of(createUserCompanyRoleDTO(company2.getUuid(), "admin-role-uuid", "ADMIN")));

        setupRoleMock("admin-role-uuid", "ADMIN", company2);

        assertThrows(CustomException.class, () -> userService.hookSaveAuth(request));
    }

    @Test
    void hookSaveAuth_RequestWithOneInvalidRoleInListFails_ThrowsException() {
        buildUserCompanyRole(currentUser, company2, "ADMIN");
        setupAuth(currentUser, company2);

        UserDTO request = new UserDTO();
        UserCompanyRoleDTO ucrDTO = new UserCompanyRoleDTO();
        ucrDTO.setCompanyUuid(company2.getUuid());
        ucrDTO.setRoles(List.of(
            createRoleDTO("OPERATOR", "op-role-uuid"), 
            createRoleDTO("SYSADMIN", "sys-role-uuid")
        ));
        request.setUserCompanyRoles(List.of(ucrDTO));

        setupRoleMock("op-role-uuid", "OPERATOR", company2);
        setupRoleMock("sys-role-uuid", "SYSADMIN", company1);

        CustomException exception = assertThrows(CustomException.class, () -> userService.hookSaveAuth(request));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
    }

    @Test
    void hookSaveAuth_CreatingUserWithNoRoles_Success() {
        buildUserCompanyRole(currentUser, company2, "ADMIN");
        setupAuth(currentUser, company2);

        UserDTO request = new UserDTO();
        request.setUuid(null);
        request.setUserCompanyRoles(List.of(createUserCompanyRoleDTO(company2.getUuid(), null, null)));

        assertDoesNotThrow(() -> userService.hookSaveAuth(request));
    }
}
