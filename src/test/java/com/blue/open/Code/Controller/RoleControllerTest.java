package com.blue.open.Code.Controller;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Core.Entity.Db.Permission;
import com.blue.open.Core.Entity.Db.Role;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Core.Entity.DTO.PermissionDTO;
import com.blue.open.Core.Entity.DTO.RoleDTO;
import com.blue.open.Core.Entity.Db.UserCompanyRole;
import com.blue.open.Core.Service.BaseRoleService;
import com.blue.open.Core.Repository.RoleRepository;
import com.blue.open.Core.Service.Auth.AuthService;
import com.blue.open.Core.Exceptions.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private BaseRoleService roleService = new BaseRoleService() {
    };

    private User currentUser;
    private Company company1; // System Company
    private Company company2; // Client Company

    private Permission permView;
    private Permission permCreate;
    private Permission permEdit;

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

        permView = new Permission();
        permView.setUuid("perm-view-uuid");
        permView.setName("VIEW_SOMETHING");

        permCreate = new Permission();
        permCreate.setUuid("perm-create-uuid");
        permCreate.setName("CREATE_SOMETHING");

        permEdit = new Permission();
        permEdit.setUuid("perm-edit-uuid");
        permEdit.setName("EDIT_SOMETHING");
    }

    private void setupAuth(User user, Company headerCompany) {
        lenient().when(authService.getActualOrThrow()).thenReturn(user);
        lenient().when(authService.getCurrentCompanyOrThrow()).thenReturn(headerCompany);
        lenient().when(authService.valid(anyList())).thenReturn(user);
        lenient().when(authService.valid()).thenReturn(user);

        // Dinámicamente devolvemos true si alguno de los roles del usuario pertenece a la empresa de sistema
        lenient().when(authService.isSysAdmin(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            if (u == null || u.getUserCompanyRoles() == null)
                return false;
            return u.getUserCompanyRoles().stream()
                    .anyMatch(ucr -> ucr.getCompany() != null &&
                            (Long.valueOf(1).equals(ucr.getCompany().getId()) || "1".equals(ucr.getCompany().getUuid())));
        });
    }

    private void setupRoleMock(String roleName, Company company, Set<Permission> permissions) {
        Role role = new Role();
        role.setName(roleName);
        role.setCompany(company);
        role.setPermissions(new ArrayList<>(permissions));
        lenient().when(roleRepository.findFirstByNameAndCompany(eq(roleName), any())).thenReturn(role);
        lenient().when(roleRepository.findFirstByNameAndCompany(eq(roleName), eq(company))).thenReturn(role);
    }

    private void buildUserCompanyRoleWithPermissions(User user, Company company, String roleName,
            Set<Permission> permissions) {
        UserCompanyRole ucr = new UserCompanyRole();
        ucr.setUser(user);
        ucr.setCompany(company);

        Role role = new Role();
        role.setName(roleName);
        role.setPermissions(new ArrayList<>(permissions));

        ucr.setRoles(List.of(role));
        user.getUserCompanyRoles().add(ucr);
    }

    private PermissionDTO createPermissionDTO(Permission p) {
        PermissionDTO dto = new PermissionDTO(p.getName());
        dto.setUuid(p.getUuid());
        return dto;
    }

    @Test
    void hookSaveAuth_NormalUserCanOnlyAssignOwnedPermissions_ThrowsException() {
        buildUserCompanyRoleWithPermissions(currentUser, company2, "MANAGER", Set.of(permView, permEdit));
        setupAuth(currentUser, company2);
        setupRoleMock("MANAGER", company2, Set.of(permView, permEdit));

        RoleDTO request = new RoleDTO();
        request.setUuid(null); 
        request.setName("New Role");
        request.setPermissions(List.of(
                createPermissionDTO(permView),
                createPermissionDTO(permCreate), // User doesn't have this!
                createPermissionDTO(permEdit)));

        assertThrows(CustomException.class, () -> roleService.hookSaveAuth(request));
    }

    @Test
    void hookSaveAuth_SystemStaffCanAssignPermissionsBasedOnCompany1_Success() {
        buildUserCompanyRoleWithPermissions(currentUser, company1, "SUPPORT", Set.of(permView, permCreate, permEdit));
        buildUserCompanyRoleWithPermissions(currentUser, company2, "GUEST", Set.of(permView));
        setupAuth(currentUser, company2); 

        RoleDTO request = new RoleDTO();
        request.setUuid(null);
        request.setName("Company 2 Staff Role");
        request.setPermissions(List.of(
                createPermissionDTO(permCreate),
                createPermissionDTO(permEdit)));

        assertDoesNotThrow(() -> roleService.hookSaveAuth(request));
    }

    @Test
    void hookSaveAuth_CreateRoleWithEmptyPermissions_Success() {
        buildUserCompanyRoleWithPermissions(currentUser, company2, "MANAGER", Set.of(permView, permEdit));
        setupAuth(currentUser, company2);
        setupRoleMock("MANAGER", company2, Set.of(permView, permEdit));

        RoleDTO request = new RoleDTO();
        request.setUuid(null);
        request.setName("Observer Role");
        request.setPermissions(new ArrayList<>()); 

        assertDoesNotThrow(() -> roleService.hookSaveAuth(request));
        assertEquals(0, request.getPermissions().size());
    }

    @Test
    void hookSaveAuth_UserWithNoPermissionsCreatesRole_ThrowsException() {
        buildUserCompanyRoleWithPermissions(currentUser, company2, "NOBODY", Set.of());
        setupAuth(currentUser, company2);
        setupRoleMock("NOBODY", company2, Set.of());

        RoleDTO request = new RoleDTO();
        request.setUuid(null);
        request.setName("Sneaky Role");
        request.setPermissions(List.of(createPermissionDTO(permView)));

        assertThrows(CustomException.class, () -> roleService.hookSaveAuth(request));
    }

    @Test
    void hookSaveAuth_NormalUserWithRolesInMultipleCompanies_UsesHeaderCompanyPermissions_ThrowsException() {
        Company company3 = new Company();
        company3.setId(3L);
        company3.setUuid("company-uuid-3");

        buildUserCompanyRoleWithPermissions(currentUser, company3, "ADMIN_C3", Set.of(permCreate));
        buildUserCompanyRoleWithPermissions(currentUser, company2, "VIEWER_C2", Set.of(permView));

        setupAuth(currentUser, company2);
        setupRoleMock("VIEWER_C2", company2, Set.of(permView));

        RoleDTO request = new RoleDTO();
        request.setUuid(null);
        request.setName("Role in C2");
        request.setPermissions(List.of(createPermissionDTO(permCreate))); 

        assertThrows(CustomException.class, () -> roleService.hookSaveAuth(request));
    }
}
