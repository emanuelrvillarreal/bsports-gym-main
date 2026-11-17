package com.blue.open.Code.Service;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Auth.RoleEnum;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Repository.CompanyRepository;
import com.blue.open.Core.Entity.Db.Permission;
import com.blue.open.Core.Entity.Db.Role;
import com.blue.open.Core.Repository.PermissionRepository;
import com.blue.open.Core.Repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClubRoleProvisioningServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private CompanyRepository companyRepository;

    private ClubRoleProvisioningService service;

    @BeforeEach
    void setUp() {
        service = new ClubRoleProvisioningService();
        ReflectionTestUtils.setField(service, "roleRepository", roleRepository);
        ReflectionTestUtils.setField(service, "permissionRepository", permissionRepository);
        ReflectionTestUtils.setField(service, "companyRepository", companyRepository);
        ReflectionTestUtils.setField(service, "roleSyncMode", "replace");
    }

    @Test
    void isTemplateRoleName_recognizesClubAndBaseRoles() {
        assertTrue(ClubRoleProvisioningService.isTemplateRoleName(RoleEnum.SOCIO_TITULAR.name()));
        assertTrue(ClubRoleProvisioningService.isTemplateRoleName("ADMIN"));
        assertFalse(ClubRoleProvisioningService.isTemplateRoleName("CUSTOM_RECEPCION"));
    }

    @Test
    void syncAllActiveCompanies_updatesSocioTitularOnEveryActiveCompany() {
        Company companyA = company("a-uuid");
        Company companyB = company("b-uuid");
        when(companyRepository.findByStatus(0)).thenReturn(List.of(companyA, companyB));

        Permission portalProfileEdit = permission(PermissionEnum.PORTAL_PROFILE_EDIT.getName(), 100L);
        stubPermissionLookups(portalProfileEdit);

        Role staleSocioA = role(RoleEnum.SOCIO_TITULAR.name(), companyA, List.of());
        Role staleSocioB = role(RoleEnum.SOCIO_TITULAR.name(), companyB, List.of());
        when(roleRepository.findFirstByNameAndCompany(RoleEnum.SOCIO_TITULAR.name(), companyA))
                .thenReturn(staleSocioA);
        when(roleRepository.findFirstByNameAndCompany(RoleEnum.SOCIO_TITULAR.name(), companyB))
                .thenReturn(staleSocioB);
        when(roleRepository.findFirstByNameAndCompany(eq("ADMIN"), any(Company.class))).thenReturn(null);
        when(roleRepository.findFirstByNameAndCompany(eq(RoleEnum.ADM_RECEPCION.name()), any(Company.class)))
                .thenReturn(null);
        when(roleRepository.findFirstByNameAndCompany(eq(RoleEnum.PROFESOR.name()), any(Company.class)))
                .thenReturn(null);
        when(roleRepository.findFirstByNameAndCompany(eq(RoleEnum.FAMILIAR.name()), any(Company.class)))
                .thenReturn(null);
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.syncAllActiveCompanies();

        ArgumentCaptor<Role> savedCaptor = ArgumentCaptor.forClass(Role.class);
        verify(roleRepository, org.mockito.Mockito.atLeast(2)).save(savedCaptor.capture());

        boolean socioUpdated = savedCaptor.getAllValues().stream()
                .filter(r -> RoleEnum.SOCIO_TITULAR.name().equals(r.getName()))
                .anyMatch(r -> r.getPermissions().stream()
                        .anyMatch(p -> PermissionEnum.PORTAL_PROFILE_EDIT.getName().equals(p.getName())));
        assertTrue(socioUpdated, "SOCIO_TITULAR debe incluir portal.profile.edit tras sync");
    }

    @Test
    void syncAllActiveCompanies_doesNotModifyCustomRole() {
        Company company = company("club-uuid");
        when(companyRepository.findByStatus(0)).thenReturn(List.of(company));

        Permission extra = permission("custom.extra", 50L);
        Role customRole = role("CUSTOM_RECEPCION", company, List.of(extra));
        when(roleRepository.findFirstByNameAndCompany("CUSTOM_RECEPCION", company)).thenReturn(customRole);

        stubPermissionLookups();
        when(roleRepository.findFirstByNameAndCompany(eq("ADMIN"), eq(company))).thenReturn(null);
        for (RoleEnum roleEnum : RoleEnum.values()) {
            when(roleRepository.findFirstByNameAndCompany(roleEnum.name(), company)).thenReturn(null);
        }
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.syncAllActiveCompanies();

        verify(roleRepository, never()).save(customRole);
        assertEquals(1, customRole.getPermissions().size());
        assertEquals("custom.extra", customRole.getPermissions().get(0).getName());
    }

    private void stubPermissionLookups(Permission... extras) {
        when(permissionRepository.findFirstByName(any())).thenAnswer(invocation -> {
            String name = invocation.getArgument(0);
            Optional<Permission> fromExtras = java.util.Arrays.stream(extras)
                    .filter(p -> p.getName().equals(name))
                    .findFirst();
            if (fromExtras.isPresent()) {
                return fromExtras.get();
            }
            for (PermissionEnum pe : PermissionEnum.values()) {
                if (pe.getName().equals(name)) {
                    return permission(name, (long) (1000 + pe.ordinal()));
                }
            }
            for (com.blue.open.Core.Auth.BasePermissionEnum bpe : com.blue.open.Core.Auth.BasePermissionEnum.values()) {
                if (bpe.getName().equals(name)) {
                    return permission(name, (long) (2000 + bpe.ordinal()));
                }
            }
            return null;
        });
    }

    private static Company company(String uuid) {
        Company c = new Company();
        c.setId(uuid.hashCode() & 0xFFFFL);
        c.setUuid(uuid);
        c.setStatus(0);
        c.setName("Club " + uuid);
        return c;
    }

    private static Role role(String name, Company company, List<Permission> permissions) {
        Role role = new Role();
        role.setName(name);
        role.setCompany(company);
        role.setPermissions(new ArrayList<>(permissions));
        return role;
    }

    private static Permission permission(String name, long id) {
        Permission p = new Permission();
        p.setId(id);
        p.setName(name);
        return p;
    }
}
