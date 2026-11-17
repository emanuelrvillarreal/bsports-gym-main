package com.blue.open.Code.Service.Personal;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Auth.RoleEnum;
import com.blue.open.Code.Entity.DTO.PersonalDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.Profesor;
import com.blue.open.Code.Entity.Db.Recepcionista;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.ProfesorEstadoEnum;
import com.blue.open.Code.Entity.Enums.RecepcionistaEstadoEnum;
import com.blue.open.Code.Repository.ActividadProfesorRepository;
import com.blue.open.Code.Repository.ProfesorRepository;
import com.blue.open.Code.Repository.RecepcionistaRepository;
import com.blue.open.Code.Service.Admin.AdminCupoService;
import com.blue.open.Code.Service.CrmPlanLimiteResolver;
import com.blue.open.Code.Service.MemberUserProvisioningService;
import com.blue.open.Core.Entity.Db.Role;
import com.blue.open.Core.Entity.Db.UserCompanyRole;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Repository.RoleRepository;
import com.blue.open.Core.Repository.UserCompanyRoleRepository;
import com.blue.open.Core.Service.Auth.AuthService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Pantalla "Personal": junta administradores, recepción y profesores en una sola lista con una fila por persona.
 * Los roles se combinan: un profe puede ser también administrador o recepcionista, y cualquiera puede además
 * ser socio (eso se maneja desde Socios con el mismo DNI y email).
 * <p>
 * Reglas: los administradores están limitados por el plan del club; nadie puede quitarse a sí mismo el rol de
 * administrador y el club nunca queda sin administradores.
 */
@Service
public class PersonalService {

    private static final String ROL_ADMIN = "ADMIN";
    private static final String ROL_RECEPCION = RoleEnum.ADM_RECEPCION.name();
    private static final String ROL_PROFESOR = RoleEnum.PROFESOR.name();
    private static final String PRODUCTO_ADMINS = "bsports-admin";
    private static final int ACTIVO = 0;
    private static final int ELIMINADO = -1;

    @Autowired
    private AuthService authService;

    @Autowired
    private AdminCupoService adminCupoService;

    @Autowired
    private CrmPlanLimiteResolver crmPlanLimiteResolver;

    @Autowired
    private MemberUserProvisioningService userProvisioningService;

    @Autowired
    private UserCompanyRoleRepository userCompanyRoleRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ProfesorRepository profesorRepository;

    @Autowired
    private RecepcionistaRepository recepcionistaRepository;

    @Autowired
    private ActividadProfesorRepository actividadProfesorRepository;

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public PersonalDTO.Listado listar(String search) {
        authService.valid(List.of(PermissionEnum.CLUB_INTERNAL_USER_MANAGE));
        Company company = authService.getCurrentCompanyOrThrow();
        String yo = authService.getActualOrThrow().getUuid();

        Map<Long, PersonalDTO.Persona> personas = new LinkedHashMap<>();
        for (User u : adminsDe(company)) {
            persona(personas, u, yo).setAdmin(true);
        }
        for (Recepcionista r : em.createQuery("""
                        SELECT r FROM Recepcionista r JOIN FETCH r.user
                        WHERE r.company.id = :companyId AND r.status = 0""", Recepcionista.class)
                .setParameter("companyId", company.getId()).getResultList()) {
            persona(personas, r.getUser(), yo).setRecepcion(true);
        }
        for (Profesor p : em.createQuery("""
                        SELECT p FROM Profesor p JOIN FETCH p.user
                        WHERE p.company.id = :companyId AND p.status = 0""", Profesor.class)
                .setParameter("companyId", company.getId()).getResultList()) {
            persona(personas, p.getUser(), yo).setProfesor(true);
        }

        if (!personas.isEmpty()) {
            for (Member m : em.createQuery("""
                            SELECT m FROM Member m JOIN FETCH m.user
                            WHERE m.company.id = :companyId AND m.status = 0 AND m.user.id IN :ids""", Member.class)
                    .setParameter("companyId", company.getId())
                    .setParameter("ids", personas.keySet())
                    .getResultList()) {
                PersonalDTO.Persona persona = personas.get(m.getUser().getId());
                if (persona != null) persona.setSocio(socio(m));
            }
        }

        String filtro = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        PersonalDTO.Listado listado = new PersonalDTO.Listado();
        listado.setPersonas(personas.values().stream()
                .filter(p -> filtro.isEmpty() || coincide(p, filtro))
                .sorted(Comparator.comparing((PersonalDTO.Persona p) -> texto(p.getApellido()))
                        .thenComparing(p -> texto(p.getNombre())))
                .toList());
        listado.setCupoAdmins(cupo(company));
        return listado;
    }

    /** Busca a alguien del sistema por DNI o email para no cargarlo dos veces (por ejemplo, si ya es socio). */
    @Transactional(readOnly = true)
    public PersonalDTO.Encontrada buscar(String q) {
        authService.valid(List.of(PermissionEnum.CLUB_INTERNAL_USER_MANAGE));
        Company company = authService.getCurrentCompanyOrThrow();
        PersonalDTO.Encontrada resultado = new PersonalDTO.Encontrada();
        String valor = q == null ? "" : q.trim();
        if (valor.isEmpty()) return resultado;

        List<User> usuarios = em.createQuery("""
                        SELECT u FROM User u
                        WHERE u.status <> -1 AND (u.dni = :valor OR LOWER(u.username) = :email OR LOWER(u.email) = :email)""",
                        User.class)
                .setParameter("valor", valor)
                .setParameter("email", valor.toLowerCase(Locale.ROOT))
                .setMaxResults(1)
                .getResultList();
        if (usuarios.isEmpty()) return resultado;

        User user = usuarios.get(0);
        // Solo se completan los datos si la persona ya tiene relación con este club: no exponemos datos de
        // usuarios de otros clubes, solo avisamos que existe.
        boolean delClub = asignacion(user, company) != null;
        resultado.setExiste(true);
        if (delClub) {
            resultado.setNombre(user.getNombre());
            resultado.setApellido(user.getApellido());
            resultado.setDni(user.getDni());
            resultado.setEmail(user.getEmail());
            resultado.setTelefono(user.getPhone());
            resultado.setYaEsPersonal(esAdmin(user, company)
                    || profesorActivo(company, user) != null
                    || recepcionistaActiva(company, user) != null);
            em.createQuery("""
                            SELECT m FROM Member m WHERE m.company.id = :companyId AND m.user.id = :userId AND m.status = 0""",
                            Member.class)
                    .setParameter("companyId", company.getId())
                    .setParameter("userId", user.getId())
                    .setMaxResults(1)
                    .getResultList()
                    .stream().findFirst().ifPresent(m -> resultado.setSocio(socio(m)));
        }
        return resultado;
    }

    @Transactional
    public PersonalDTO.Listado alta(PersonalDTO.Alta command) {
        authService.valid(List.of(PermissionEnum.CLUB_INTERNAL_USER_MANAGE));
        Company company = authService.getCurrentCompanyOrThrow();
        if (command == null || (!command.isAdmin() && !command.isRecepcion() && !command.isProfesor())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Elegí al menos un rol",
                    "PersonalService.alta | sin roles");
        }
        if (vacio(command.getNombre()) || vacio(command.getApellido()) || vacio(command.getEmail())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Completá nombre, apellido y email",
                    "PersonalService.alta | datos incompletos");
        }

        String primerRol = command.isAdmin() ? ROL_ADMIN : command.isProfesor() ? ROL_PROFESOR : ROL_RECEPCION;
        if (command.isAdmin()) adminCupoService.validarCupoDisponible(company, 1);
        User user = userProvisioningService.resolveOrCreateMigrationUser(command, company, rol(company, primerRol));

        PersonalDTO.Roles roles = new PersonalDTO.Roles();
        roles.setAdmin(command.isAdmin());
        roles.setRecepcion(command.isRecepcion() && !command.isAdmin());
        roles.setProfesor(command.isProfesor());
        aplicar(company, user, roles, true);
        return listar(null);
    }

    @Transactional
    public PersonalDTO.Listado cambiarRoles(String userUuid, PersonalDTO.Roles roles) {
        authService.valid(List.of(PermissionEnum.CLUB_INTERNAL_USER_MANAGE));
        Company company = authService.getCurrentCompanyOrThrow();
        if (roles == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Datos requeridos", "PersonalService | roles null");
        }
        List<User> usuarios = em.createQuery("SELECT u FROM User u WHERE u.uuid = :uuid", User.class)
                .setParameter("uuid", userUuid).getResultList();
        if (usuarios.isEmpty() || asignacion(usuarios.get(0), company) == null) {
            throw new CustomException(HttpStatus.NOT_FOUND, "Persona no encontrada",
                    "PersonalService | userUuid: " + userUuid);
        }
        aplicar(company, usuarios.get(0), roles, false);
        return listar(null);
    }

    private void aplicar(Company company, User user, PersonalDTO.Roles roles, boolean alta) {
        boolean eraAdmin = esAdmin(user, company);
        if (roles.isAdmin() && !eraAdmin) {
            if (!alta) adminCupoService.validarCupoDisponible(company, 1);
            agregarRol(company, user, ROL_ADMIN);
        } else if (!roles.isAdmin() && eraAdmin) {
            quitarAdmin(company, user);
        }

        // Administrador ya incluye todo lo de recepción.
        boolean quiereRecepcion = roles.isRecepcion() && !roles.isAdmin();
        Recepcionista recepcionista = recepcionistaActiva(company, user);
        if (quiereRecepcion && recepcionista == null) {
            altaRecepcion(company, user);
        } else if (!quiereRecepcion && recepcionista != null) {
            recepcionista.setEstado(RecepcionistaEstadoEnum.INACTIVO);
            recepcionista.setStatus(ELIMINADO);
            recepcionistaRepository.save(recepcionista);
            quitarRol(company, user, ROL_RECEPCION);
        }

        Profesor profesor = profesorActivo(company, user);
        if (roles.isProfesor() && profesor == null) {
            altaProfesor(company, user);
        } else if (!roles.isProfesor() && profesor != null) {
            actividadProfesorRepository.bajaLogicaByProfesorId(profesor.getId());
            profesor.setEstado(ProfesorEstadoEnum.INACTIVO);
            profesor.setStatus(ELIMINADO);
            profesorRepository.save(profesor);
            quitarRol(company, user, ROL_PROFESOR);
        }
    }

    private void quitarAdmin(Company company, User user) {
        if (Objects.equals(user.getUuid(), authService.getActualOrThrow().getUuid())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "No podés quitarte a vos mismo el rol de administrador",
                    "PersonalService | auto-quita de ADMIN");
        }
        if (userCompanyRoleRepository.findByCompanyAndRoleName(company, ROL_ADMIN).size() <= 1) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "El gimnasio no puede quedarse sin administradores",
                    "PersonalService | último ADMIN");
        }
        quitarRol(company, user, ROL_ADMIN);
    }

    private void altaRecepcion(Company company, User user) {
        Recepcionista r = recepcionistaRepository.findFirstByCompany_IdAndUser_Id(company.getId(), user.getId())
                .orElseGet(Recepcionista::new);
        r.setCompany(company);
        r.setUser(user);
        r.setStatus(ACTIVO);
        r.setEstado(RecepcionistaEstadoEnum.ACTIVO);
        r.setFechaAlta(LocalDate.now());
        recepcionistaRepository.save(r);
        agregarRol(company, user, ROL_RECEPCION);
    }

    private void altaProfesor(Company company, User user) {
        Profesor p = profesorRepository.findFirstByCompany_IdAndUser_Id(company.getId(), user.getId())
                .orElseGet(Profesor::new);
        p.setCompany(company);
        p.setUser(user);
        p.setStatus(ACTIVO);
        p.setEstado(ProfesorEstadoEnum.ACTIVO);
        p.setFechaAlta(LocalDate.now());
        profesorRepository.save(p);
        agregarRol(company, user, ROL_PROFESOR);
    }

    private void agregarRol(Company company, User user, String nombreRol) {
        Role role = rol(company, nombreRol);
        UserCompanyRole ucr = asignacionCualquierEstado(user, company);
        if (ucr == null) {
            ucr = new UserCompanyRole();
            ucr.setUser(user);
            ucr.setCompany(company);
            ucr.setRoles(new ArrayList<>());
        }
        ucr.setStatus(ACTIVO);
        if (ucr.getRoles() == null) ucr.setRoles(new ArrayList<>());
        if (ucr.getRoles().stream().noneMatch(r -> nombreRol.equals(r.getName()))) {
            ucr.getRoles().add(role);
        }
        userCompanyRoleRepository.save(ucr);
    }

    private void quitarRol(Company company, User user, String nombreRol) {
        UserCompanyRole ucr = asignacion(user, company);
        if (ucr == null || ucr.getRoles() == null) return;
        ucr.getRoles().removeIf(r -> nombreRol.equals(r.getName()));
        if (ucr.getRoles().isEmpty()) ucr.setStatus(ELIMINADO);
        userCompanyRoleRepository.save(ucr);
    }

    private Role rol(Company company, String nombre) {
        Role role = roleRepository.findFirstByNameAndCompany(nombre, company);
        if (role == null) {
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "Configuración incompleta",
                    "PersonalService | rol " + nombre + " no encontrado para company " + company.getId());
        }
        return role;
    }

    private List<User> adminsDe(Company company) {
        return em.createQuery("""
                        SELECT DISTINCT u FROM UserCompanyRole ucr JOIN ucr.user u JOIN ucr.roles r
                        WHERE ucr.company.id = :companyId AND r.name = :rol AND ucr.status = 0""", User.class)
                .setParameter("companyId", company.getId())
                .setParameter("rol", ROL_ADMIN)
                .getResultList();
    }

    private boolean esAdmin(User user, Company company) {
        return userCompanyRoleRepository
                .findByCompanyIdAndUserUuidAndRoleName(company.getId(), user.getUuid(), ROL_ADMIN).isPresent();
    }

    private UserCompanyRole asignacion(User user, Company company) {
        return em.createQuery("""
                        SELECT ucr FROM UserCompanyRole ucr LEFT JOIN FETCH ucr.roles
                        WHERE ucr.company.id = :companyId AND ucr.user.id = :userId AND ucr.status = 0""",
                        UserCompanyRole.class)
                .setParameter("companyId", company.getId())
                .setParameter("userId", user.getId())
                .getResultList().stream().findFirst().orElse(null);
    }

    private UserCompanyRole asignacionCualquierEstado(User user, Company company) {
        UserCompanyRole activa = asignacion(user, company);
        if (activa != null) return activa;
        return em.createQuery("""
                        SELECT ucr FROM UserCompanyRole ucr LEFT JOIN FETCH ucr.roles
                        WHERE ucr.company.id = :companyId AND ucr.user.id = :userId ORDER BY ucr.id DESC""",
                        UserCompanyRole.class)
                .setParameter("companyId", company.getId())
                .setParameter("userId", user.getId())
                .getResultList().stream().findFirst().orElse(null);
    }

    private Profesor profesorActivo(Company company, User user) {
        return profesorRepository.findFirstByCompany_IdAndUser_Id(company.getId(), user.getId())
                .filter(p -> p.getStatus() == null || p.getStatus() == ACTIVO)
                .orElse(null);
    }

    private Recepcionista recepcionistaActiva(Company company, User user) {
        return recepcionistaRepository.findFirstByCompany_IdAndUser_Id(company.getId(), user.getId())
                .filter(r -> r.getStatus() == null || r.getStatus() == ACTIVO)
                .orElse(null);
    }

    private PersonalDTO.Cupo cupo(Company company) {
        PersonalDTO.Cupo cupo = new PersonalDTO.Cupo();
        cupo.setActuales(userCompanyRoleRepository.findByCompanyAndRoleName(company, ROL_ADMIN).size());
        cupo.setLimite(crmPlanLimiteResolver.resolverLimite(company, PRODUCTO_ADMINS).orElse(null));
        return cupo;
    }

    private static PersonalDTO.Persona persona(Map<Long, PersonalDTO.Persona> personas, User u, String yo) {
        return personas.computeIfAbsent(u.getId(), id -> {
            PersonalDTO.Persona p = new PersonalDTO.Persona();
            p.setUserUuid(u.getUuid());
            p.setNombre(u.getNombre());
            p.setApellido(u.getApellido());
            p.setDni(u.getDni());
            p.setEmail(u.getEmail());
            p.setTelefono(u.getPhone());
            p.setImageUrl(u.getImageUrl());
            p.setYo(Objects.equals(u.getUuid(), yo));
            return p;
        });
    }

    private static PersonalDTO.Socio socio(Member m) {
        PersonalDTO.Socio s = new PersonalDTO.Socio();
        s.setMemberUuid(m.getUuid());
        s.setNumeroSocio(m.getNumeroSocio());
        return s;
    }

    private static boolean coincide(PersonalDTO.Persona p, String filtro) {
        String todo = String.join(" ", texto(p.getNombre()), texto(p.getApellido()), texto(p.getDni()),
                texto(p.getEmail()));
        for (String palabra : filtro.split("\\s+")) {
            if (!todo.contains(palabra)) return false;
        }
        return true;
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor.toLowerCase(Locale.ROOT);
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
