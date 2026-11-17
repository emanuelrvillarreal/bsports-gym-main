package com.blue.open;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Core.Entity.Db.Role;
import com.blue.open.Core.Entity.Db.UserCompanyRole;
import com.blue.open.Core.Repository.BaseCompanyRepository;
import com.blue.open.Core.Repository.RoleRepository;
import com.blue.open.Core.Repository.UserRepository;
import com.blue.open.Core.Service.Auth.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private BaseCompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private AuthService authService;

    private User localAdmin;
    private Company company1;
    private Company company2;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();
        companyRepository.deleteAll();

        // 1. Setup Companies
        company1 = new Company();
        company1.setName("System Co");
        company1.setUuid("1");
        company1 = companyRepository.save(company1);

        company2 = new Company();
        company2.setName("Client Co 2");
        company2.setUuid("uuid-co-2");
        company2 = companyRepository.save(company2);

        // 2. Setup Roles
        // System Role (MANAGER)
        Role sysRole = new Role();
        sysRole.setName("MANAGER");
        sysRole.setCompany(company1);
        sysRole.setStatus(0);
        roleRepository.save(sysRole);

        // Local Role
        Role localRole = new Role();
        localRole.setName("LOCAL_WORKER");
        localRole.setCompany(company2);
        localRole.setStatus(0);
        roleRepository.save(localRole);

        // 3. Setup User
        localAdmin = new User();
        localAdmin.setUsername("admin2");
        localAdmin.setEmail("admin@co2.com");
        localAdmin.setStatus(0);
        
        UserCompanyRole ucr = new UserCompanyRole();
        ucr.setCompany(company2);
        Role adminRole = new Role();
        adminRole.setName("ADMIN");
        adminRole.setCompany(company2);
        adminRole.setStatus(0);
        adminRole = roleRepository.save(adminRole);
        
        ucr.setRoles(List.of(adminRole));
        localAdmin.setUserCompanyRoles(new ArrayList<>(List.of(ucr)));
        localAdmin = userRepository.save(localAdmin);
    }

    @Test
    void paginateRoles_AsLocalAdmin_ShouldSeeSystemRolesInEnumAndLocalRoles() throws Exception {
        // Mock Auth context
        when(authService.getActualOrThrow()).thenReturn(localAdmin);
        when(authService.getCurrentCompanyOrThrow()).thenReturn(company2);
        when(authService.isSysAdmin(org.mockito.ArgumentMatchers.any())).thenReturn(false);
        when(authService.valid()).thenReturn(localAdmin);
        when(authService.isSysAdmin(localAdmin)).thenReturn(false);

        Map<String, Object> request = new HashMap<>();
        request.put("page", 1);
        request.put("size", 10);

        mockMvc.perform(post("/role/paginate")
                .header("X-Company-Uuid", "uuid-co-2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request)))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk());
    }
}
