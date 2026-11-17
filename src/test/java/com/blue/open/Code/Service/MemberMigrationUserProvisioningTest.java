package com.blue.open.Code.Service;

import com.blue.open.Code.Auth.RoleEnum;
import com.blue.open.Code.Entity.DTO.MemberTitularAltaCommand;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Mapper.MemberMapper;
import com.blue.open.Core.Entity.Db.Role;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Repository.RoleRepository;
import com.blue.open.Core.Repository.UserCompanyRoleRepository;
import com.blue.open.Core.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberMigrationUserProvisioningTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserCompanyRoleRepository userCompanyRoleRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private MemberUserProvisioningService service;
    private Company company;

    @BeforeEach
    void setUp() {
        MemberMapper memberMapper = new MemberMapper();
        MemberUserValidationHelper validationHelper = new MemberUserValidationHelper();
        ReflectionTestUtils.setField(validationHelper, "userRepository", userRepository);

        service = new MemberUserProvisioningService();
        ReflectionTestUtils.setField(service, "userRepository", userRepository);
        ReflectionTestUtils.setField(service, "userCompanyRoleRepository", userCompanyRoleRepository);
        ReflectionTestUtils.setField(service, "roleRepository", roleRepository);
        ReflectionTestUtils.setField(service, "memberMapper", memberMapper);
        ReflectionTestUtils.setField(service, "validationHelper", validationHelper);
        ReflectionTestUtils.setField(service, "passwordEncoder", passwordEncoder);

        company = new Company();
        company.setId(1L);
        company.setUuid("club");
    }

    @Test
    void resolveOrCreateMigrationUser_reutilizaUsuarioExistentePorDniYEmail() {
        User active = new User();
        active.setId(20L);
        active.setUuid("uuid-20");
        active.setStatus(0);
        active.setUserCompanyRoles(new ArrayList<>());
        when(userRepository.findFirstByDniAndUsernameIgnoreCase("30111222", "activo@test.com"))
                .thenReturn(active);
        when(roleRepository.findFirstByNameAndCompany(RoleEnum.SOCIO_TITULAR.name(), company))
                .thenReturn(new Role());
        when(userRepository.findFirstByUuidWithDetails("uuid-20")).thenReturn(active);
        when(userCompanyRoleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MemberTitularAltaCommand command = command("activo@test.com", "30111222");

        User resolved = service.resolveOrCreateMigrationUser(command, company, RoleEnum.SOCIO_TITULAR);

        assertEquals("uuid-20", resolved.getUuid());
        verify(roleRepository).findFirstByNameAndCompany(RoleEnum.SOCIO_TITULAR.name(), company);
    }

    @Test
    void resolveOrCreateMigrationUser_creaSiNoExiste() {
        when(userRepository.findFirstByDniAndUsernameIgnoreCase("30111222", "nuevo@test.com"))
                .thenReturn(null);
        when(userRepository.findFirstByUsernameIgnoringCase("nuevo@test.com")).thenReturn(null);
        when(userRepository.findFirstByDni("30111222")).thenReturn(null);
        when(roleRepository.findFirstByNameAndCompany(RoleEnum.SOCIO_TITULAR.name(), company))
                .thenReturn(new Role());
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(99L);
            return u;
        });
        when(userCompanyRoleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User created = service.resolveOrCreateMigrationUser(command("nuevo@test.com", "30111222"),
                company, RoleEnum.SOCIO_TITULAR);

        assertEquals(99L, created.getId());
        verify(userRepository, org.mockito.Mockito.atLeastOnce()).save(any(User.class));
    }

    @Test
    void resolveOrCreateMigrationUser_creaSiNoExisteSinDni() {
        when(userRepository.findFirstByUsernameIgnoringCase("nuevo@test.com")).thenReturn(null);
        when(roleRepository.findFirstByNameAndCompany(RoleEnum.SOCIO_TITULAR.name(), company))
                .thenReturn(new Role());
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(101L);
            return u;
        });
        when(userCompanyRoleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        User created = service.resolveOrCreateMigrationUser(command("nuevo@test.com", null),
                company, RoleEnum.SOCIO_TITULAR);

        assertEquals(101L, created.getId());
    }

    @Test
    void resolveOrCreateMigrationUser_throwSiEmailPerteneceAUsuarioConOtroDni() {
        User activePorEmail = new User();
        activePorEmail.setId(10L);
        activePorEmail.setUuid("uuid-10");
        activePorEmail.setStatus(0);
        when(userRepository.findFirstByDniAndUsernameIgnoreCase("30111222", "mix@test.com"))
                .thenReturn(null);
        when(userRepository.findFirstByUsernameIgnoringCase("mix@test.com")).thenReturn(activePorEmail);

        CustomException ex = assertThrows(CustomException.class,
                () -> service.resolveOrCreateMigrationUser(command("mix@test.com", "30111222"),
                        company, RoleEnum.SOCIO_TITULAR));

        assertEquals("El correo electrónico ya está en uso por un usuario con un DNI distinto al ingresado.",
                ex.getUserMessage());
    }

    @Test
    void resolveOrCreateMigrationUser_throwSiDniPerteneceAUsuarioConOtroEmail() {
        User activePorDni = new User();
        activePorDni.setId(11L);
        activePorDni.setUuid("uuid-11");
        activePorDni.setStatus(0);
        when(userRepository.findFirstByDniAndUsernameIgnoreCase("30111222", "otro@test.com"))
                .thenReturn(null);
        when(userRepository.findFirstByUsernameIgnoringCase("otro@test.com")).thenReturn(null);
        when(userRepository.findFirstByDni("30111222")).thenReturn(activePorDni);

        CustomException ex = assertThrows(CustomException.class,
                () -> service.resolveOrCreateMigrationUser(command("otro@test.com", "30111222"),
                        company, RoleEnum.SOCIO_TITULAR));

        assertEquals("Ya existe un usuario registrado con ese DNI.", ex.getUserMessage());
    }

    private MemberTitularAltaCommand command(String email, String dni) {
        MemberTitularAltaCommand command = new MemberTitularAltaCommand();
        command.setNombre("Test");
        command.setApellido("User");
        command.setEmail(email);
        command.setDni(dni);
        command.setPassword("Pass1234");
        command.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        return command;
    }
}
