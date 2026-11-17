package com.blue.open.Code.Mapper;

import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.DTO.UserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserMapperTest {

    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapper();
    }

    @Test
    void modelToDto_and_dtoToModel_roundTripProfileFields() {
        User model = new User();
        model.setUuid("user-uuid-1");
        model.setUserCompanyRoles(null);
        model.setNombre("Juan");
        model.setApellido("Pérez");
        model.setDni("30123456");
        model.setFechaNacimiento(LocalDate.of(1990, 5, 15));
        model.setImageUrl("https://cdn.example/photo.jpg");
        model.setPhone("1155551234");
        model.setEmail("juan@example.com");

        UserDTO dto = userMapper.modelToDto(model);

        assertEquals("Juan", dto.getNombre());
        assertEquals("Pérez", dto.getApellido());
        assertEquals("30123456", dto.getDni());
        assertEquals(LocalDate.of(1990, 5, 15), dto.getFechaNacimiento());
        assertEquals("https://cdn.example/photo.jpg", dto.getImageUrl());
        assertEquals("1155551234", dto.getTelefono());

        dto.setNombre("Juana");
        dto.setApellido("García");
        dto.setTelefono("1144445678");

        User mapped = userMapper.dtoToModel(dto, new User());

        assertEquals("Juana", mapped.getNombre());
        assertEquals("García", mapped.getApellido());
        assertEquals("1144445678", mapped.getPhone());
        assertEquals("Juana García", mapped.getName());
    }

    @Test
    void dtoToModel_syncDisplayName_onlyNombre() {
        UserDTO dto = new UserDTO();
        dto.setNombre("Ana");

        User mapped = userMapper.dtoToModel(dto, new User());

        assertEquals("Ana", mapped.getName());
        assertNull(mapped.getApellido());
    }
}
