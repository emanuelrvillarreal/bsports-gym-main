package com.blue.open.Core.Service;

import com.blue.open.Core.Mapper.RoleMapper;
import com.blue.open.Core.Entity.DTO.PermissionDTO;
import com.blue.open.Core.Entity.DTO.RoleDTO;
import com.blue.open.Core.Entity.Db.Permission;
import com.blue.open.Core.Entity.Db.Role;
import com.blue.open.Core.Repository.PermissionRepository;
import com.blue.open.Core.Repository.RoleRepository;
import com.blue.open.Core.Service.Auth.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @InjectMocks
    private BaseRoleService roleService;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private RoleMapper roleMapper;

    @Mock
    private AuthService authService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        org.springframework.test.util.ReflectionTestUtils.setField(roleService, "mapper", roleMapper);
        org.springframework.test.util.ReflectionTestUtils.setField(roleService, "roleRepository", roleRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(roleService, "repository", roleRepository);
        org.springframework.test.util.ReflectionTestUtils.setField(roleService, "authService", authService);
    }

    @Test
    void hookSaveMapperDtoToModel_LoadsPermissionsFromDb() {
        // ARRANGE
        RoleDTO dto = new RoleDTO();
        PermissionDTO pDto = new PermissionDTO();
        pDto.setUuid("permission-uuid");
        dto.setPermissions(List.of(pDto));

        Role model = new Role();

        Permission dbPermission = new Permission();
        dbPermission.setUuid("permission-uuid");

        // Mock the mapper call in AbstractService.hookSaveMapperDtoToModel
        when(roleMapper.dtoToModel(any(), any())).thenReturn(model);
        when(permissionRepository.findFirstByUuid("permission-uuid")).thenReturn(dbPermission);

        // ACT
        Role result = roleService.hookSaveMapperDtoToModel(dto, model);

        // ASSERT
        // Verify that it tried to load the permission from the database
        verify(permissionRepository).findFirstByUuid("permission-uuid");

        // Verify that the resulting model has the persistent permission attached
        assertEquals(1, result.getPermissions().size());
        assertEquals(dbPermission, result.getPermissions().get(0));
    }

    @Test
    void hookSaveDatabaseSave_ClearsOtherDefaultsWhenCurrentIsDefault() {
        // ARRANGE
        Role model = new Role();
        model.setIsDefault(1);
        model.setUuid("new-default-uuid");
        com.blue.open.Code.Entity.Db.Company company = new com.blue.open.Code.Entity.Db.Company();
        model.setCompany(company);

        Role existingDefault = new Role();
        existingDefault.setUuid("old-default-uuid");
        existingDefault.setIsDefault(1);

        when(roleRepository.findAllByCompanyAndIsDefault(company, 1)).thenReturn(List.of(existingDefault));
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // ACT
        roleService.hookSaveDatabaseSave(model);

        // ASSERT
        // Verify that the existing default role was updated and saved
        assertEquals(0, existingDefault.getIsDefault());
        verify(roleRepository).save(existingDefault);
        // Verify the main model was also saved (by super call)
        verify(roleRepository).save(model);
    }
}
