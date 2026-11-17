package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.DTO.UserDTO;
import com.blue.open.Code.Mapper.UserMapper;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceProfileTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MemberUserValidationHelper memberUserValidationHelper;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService();
        ReflectionTestUtils.setField(userService, "mapper", new UserMapper());
        ReflectionTestUtils.setField(userService, "repository", userRepository);
        ReflectionTestUtils.setField(userService, "passwordEncoder", passwordEncoder);
        ReflectionTestUtils.setField(userService, "memberUserValidationHelper", memberUserValidationHelper);
    }

    @Test
    void hookSaveMapperDtoToModel_syncsUsernameWithEmail() {
        UserDTO dto = new UserDTO();
        dto.setEmail("  socio@club.com  ");
        dto.setNombre("Carlos");
        dto.setApellido("López");

        User model = userService.hookSaveMapperDtoToModel(dto, new User());

        assertEquals("socio@club.com", model.getUsername());
        assertEquals("socio@club.com", model.getEmail());
        assertEquals("Carlos López", model.getName());
    }

    @Test
    void hookSaveMapperDtoToModel_rejectsDuplicateEmail() {
        UserDTO dto = new UserDTO();
        dto.setUuid("new-user-uuid");
        dto.setEmail("duplicado@club.com");

        User existing = new User();
        existing.setUuid("other-uuid");
        when(userRepository.findFirstByEmailAndStatusNot(eq("duplicado@club.com"), eq(-1)))
                .thenReturn(existing);

        assertThrows(CustomException.class, () -> userService.hookSaveMapperDtoToModel(dto, new User()));
    }

    @Test
    void hookSaveMapperDtoToModel_allowsSameEmailOnEdit() {
        UserDTO dto = new UserDTO();
        dto.setUuid("same-uuid");
        dto.setEmail("mismo@club.com");

        User existing = new User();
        existing.setUuid("same-uuid");
        when(userRepository.findFirstByEmailAndStatusNot(eq("mismo@club.com"), eq(-1)))
                .thenReturn(existing);

        User model = userService.hookSaveMapperDtoToModel(dto, new User());

        assertEquals("mismo@club.com", model.getUsername());
    }

    @Test
    void hookSaveMapperDtoToModel_rejectsDuplicateDni() {
        UserDTO dto = new UserDTO();
        dto.setUuid("new-user-uuid");
        dto.setDni("30111222");

        org.mockito.Mockito.doThrow(new CustomException(org.springframework.http.HttpStatus.BAD_REQUEST,
                "DNI ya registrado", "Ya existe un usuario con ese DNI."))
                .when(memberUserValidationHelper).validateDniUnique("30111222", "new-user-uuid");

        assertThrows(CustomException.class, () -> userService.hookSaveMapperDtoToModel(dto, new User()));
    }

    @Test
    void hookSaveMapperDtoToModel_rejectsInvalidDni() {
        UserDTO dto = new UserDTO();
        dto.setDni("ABC123");

        assertThrows(CustomException.class, () -> userService.hookSaveMapperDtoToModel(dto, new User()));
    }
}
