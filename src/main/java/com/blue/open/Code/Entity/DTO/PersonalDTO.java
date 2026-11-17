package com.blue.open.Code.Entity.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Pantalla "Personal": una fila por persona del club con sus roles (administrador, recepción, profesor) y si
 * además es socio. Una misma persona puede tener varios roles.
 */
public final class PersonalDTO {

    private PersonalDTO() {
    }

    @Getter
    @Setter
    public static class Listado {
        private Cupo cupoAdmins = new Cupo();
        private List<Persona> personas = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class Cupo {
        private long actuales;
        /** Null si el plan no informa un límite. */
        private Integer limite;
    }

    @Getter
    @Setter
    public static class Persona {
        private String userUuid;
        private String nombre;
        private String apellido;
        private String dni;
        private String email;
        private String telefono;
        private String imageUrl;
        private boolean admin;
        private boolean recepcion;
        private boolean profesor;
        /** Es la persona que tiene la sesión abierta (no puede quitarse el rol de administrador). */
        private boolean yo;
        private Socio socio;
    }

    @Getter
    @Setter
    public static class Socio {
        private String memberUuid;
        private String numeroSocio;
    }

    @Getter
    @Setter
    public static class Roles {
        private boolean admin;
        private boolean recepcion;
        private boolean profesor;
    }

    /** Alta de una persona nueva o existente (se reconoce por DNI + email) con los roles elegidos. */
    @Getter
    @Setter
    public static class Alta extends MemberAltaUserFields {
        private boolean admin;
        private boolean recepcion;
        private boolean profesor;
    }

    /** Resultado de buscar por DNI o email antes de cargar a alguien. */
    @Getter
    @Setter
    public static class Encontrada {
        private boolean existe;
        private String nombre;
        private String apellido;
        private String dni;
        private String email;
        private String telefono;
        private boolean yaEsPersonal;
        private Socio socio;
    }
}
