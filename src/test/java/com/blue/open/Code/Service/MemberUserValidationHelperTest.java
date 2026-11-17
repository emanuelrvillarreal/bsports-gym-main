package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberUserValidationHelperTest {

    @Mock
    private UserRepository userRepository;

    private MemberUserValidationHelper validationHelper;

    @BeforeEach
    void setUp() {
        validationHelper = new MemberUserValidationHelper();
        ReflectionTestUtils.setField(validationHelper, "userRepository", userRepository);
    }

    @Test
    void validateDniUnique_rechazaDniDeUsuarioNoEliminado() {
        User existing = new User();
        existing.setUuid("other-uuid");
        when(userRepository.findFirstByDniAndStatusNot(eq("30111222"), eq(-1))).thenReturn(existing);

        CustomException ex = assertThrows(CustomException.class,
                () -> validationHelper.validateDniUnique("30111222", null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("DNI ya registrado", ex.getUserMessage());
    }

    @Test
    void validateDniUnique_permiteSiSoloExisteEliminado() {
        when(userRepository.findFirstByDniAndStatusNot(eq("30111222"), eq(-1))).thenReturn(null);

        assertDoesNotThrow(() -> validationHelper.validateDniUnique("30111222", null));
    }

    @Test
    void validateDniUnique_permiteMismoDniAlEditar() {
        User existing = new User();
        existing.setUuid("same-uuid");
        when(userRepository.findFirstByDniAndStatusNot(eq("30111222"), eq(-1))).thenReturn(existing);

        assertDoesNotThrow(() -> validationHelper.validateDniUnique("30111222", "same-uuid"));
    }
}
