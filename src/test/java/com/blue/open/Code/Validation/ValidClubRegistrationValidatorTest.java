package com.blue.open.Code.Validation;

import com.blue.open.Code.Entity.DTO.ClubAdminRequestDTO;
import com.blue.open.Code.Entity.DTO.ClubRegistrationRequestDTO;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Core.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidClubRegistrationValidatorTest {

    @Mock
    private UserRepository userRepository;

    private ValidClubRegistrationValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ValidClubRegistrationValidator();
        ReflectionTestUtils.setField(validator, "userRepository", userRepository);
    }

    @Test
    void rejectsSameInstitutionalAndAdminEmail() {
        ClubRegistrationRequestDTO request = new ClubRegistrationRequestDTO();
        request.setEmailInstitucional("admin@catnorte.com");
        ClubAdminRequestDTO admin = new ClubAdminRequestDTO();
        admin.setEmail("admin@catnorte.com");
        request.setAdmin(admin);

        assertFalse(validator.isValid(request, ValidationTestSupport.mockContext()));
    }

    @Test
    void rejectsDuplicateAdminEmail() {
        ClubRegistrationRequestDTO request = new ClubRegistrationRequestDTO();
        request.setEmailInstitucional("info@catnorte.com");
        ClubAdminRequestDTO admin = new ClubAdminRequestDTO();
        admin.setEmail("admin@catnorte.com");
        request.setAdmin(admin);
        when(userRepository.findFirstByEmailAndStatusNot("admin@catnorte.com", -1)).thenReturn(new User());

        assertFalse(validator.isValid(request, ValidationTestSupport.mockContext()));
    }

    @Test
    void acceptsUniqueAdminEmail() {
        ClubRegistrationRequestDTO request = new ClubRegistrationRequestDTO();
        request.setEmailInstitucional("info@catnorte.com");
        ClubAdminRequestDTO admin = new ClubAdminRequestDTO();
        admin.setEmail("admin@catnorte.com");
        request.setAdmin(admin);
        when(userRepository.findFirstByEmailAndStatusNot("admin@catnorte.com", -1)).thenReturn(null);

        assertTrue(validator.isValid(request, ValidationTestSupport.mockContext()));
    }
}
