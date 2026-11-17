package com.blue.open.Code.Service.Entrenamiento;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.EjercicioDTO;
import com.blue.open.Code.Entity.DTO.RutinaDTO;
import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Ejercicio;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.Profesor;
import com.blue.open.Code.Entity.Db.Rutina;
import com.blue.open.Code.Entity.Db.RutinaAsignacion;
import com.blue.open.Code.Entity.Db.RutinaDia;
import com.blue.open.Code.Entity.Db.RutinaHistorial;
import com.blue.open.Code.Entity.Db.RutinaItem;
import com.blue.open.Code.Entity.Db.RutinaSeguimiento;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.AsignacionRutinaEstadoEnum;
import com.blue.open.Code.Entity.Enums.DificultadEjercicioEnum;
import com.blue.open.Code.Entity.Enums.RutinaEstadoEnum;
import com.blue.open.Code.Entity.Enums.TipoEjercicioEnum;
import com.blue.open.Code.Repository.ActividadProfesorRepository;
import com.blue.open.Code.Repository.ActividadRepository;
import com.blue.open.Code.Repository.EjercicioRepository;
import com.blue.open.Code.Repository.ProfesorRepository;
import com.blue.open.Code.Repository.RutinaAsignacionRepository;
import com.blue.open.Code.Repository.RutinaDiaRepository;
import com.blue.open.Code.Repository.RutinaHistorialRepository;
import com.blue.open.Code.Repository.RutinaItemRepository;
import com.blue.open.Code.Repository.RutinaRepository;
import com.blue.open.Code.Repository.RutinaSeguimientoRepository;
import com.blue.open.Code.Service.MemberAuthService;
import com.blue.open.Code.Service.ProfesorAuthService;
import com.blue.open.Core.Auth.BaseRoleEnum;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Modulo de entrenamiento: biblioteca de ejercicios, rutinas por dias, asignaciones a socios,
 * seguimiento de sesiones e historial de cambios.
 *
 * Alcance (siempre dentro del club del header X-Company-Uuid):
 * - ADMIN: todo el club.
 * - PROFESOR: solo las rutinas a su cargo, y solo puede asignarlas a socios con inscripcion activa en
 *   alguna de sus actividades. La biblioteca de ejercicios es comun al club; cada ejercicio lo edita
 *   quien lo cargo o un ADMIN.
 * - Socio: solo consulta sus rutinas y las de su grupo familiar (portal).
 * Las ediciones no borran: los dias y ejercicios anteriores quedan con baja logica, asi el historial
 * de sesiones sigue apuntando a lo que se hizo.
 */
@Service
public class EntrenamientoService {

    private static final int MAX_ALUMNOS = 200;

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private AuthService authService;

    @Autowired
    private ProfesorAuthService profesorAuthService;

    @Autowired
    private MemberAuthService memberAuthService;

    @Autowired
    private ActividadRepository actividadRepository;

    @Autowired
    private ActividadProfesorRepository actividadProfesorRepository;

    @Autowired
    private ProfesorRepository profesorRepository;

    @Autowired
    private EjercicioRepository ejercicioRepository;

    @Autowired
    private RutinaRepository rutinaRepository;

    @Autowired
    private RutinaDiaRepository diaRepository;

    @Autowired
    private RutinaItemRepository itemRepository;

    @Autowired
    private RutinaAsignacionRepository asignacionRepository;

    @Autowired
    private RutinaSeguimientoRepository seguimientoRepository;

    @Autowired
    private RutinaHistorialRepository historialRepository;

    /** Quien opera y sobre que: el profesor es null para ADMIN. */
    private record Alcance(Company company, User user, Profesor profesor, Set<Long> actividades) {
        boolean esAdmin() {
            return profesor == null;
        }
    }

    // ================================================================ Ejercicios

    @Transactional(readOnly = true)
    public Page<EjercicioDTO.Respuesta> listarEjercicios(
            String search, String disciplina, TipoEjercicioEnum tipo, DificultadEjercicioEnum dificultad, int page, int size) {
        Alcance alcance = alcance();
        StringBuilder where = new StringBuilder(" FROM Ejercicio e LEFT JOIN e.creadoPor u WHERE e.company.id = :c AND e.status = 0");
        Map<String, Object> params = new HashMap<>();
        params.put("c", alcance.company().getId());
        if (tieneTexto(search)) {
            where.append(" AND (LOWER(e.nombre) LIKE :s OR LOWER(e.grupoMuscular) LIKE :s OR LOWER(e.descripcion) LIKE :s)");
            params.put("s", like(search));
        }
        if (tieneTexto(disciplina)) {
            where.append(" AND LOWER(e.disciplina) LIKE :d");
            params.put("d", like(disciplina));
        }
        if (tipo != null) {
            where.append(" AND e.tipo = :t");
            params.put("t", tipo);
        }
        if (dificultad != null) {
            where.append(" AND e.dificultad = :df");
            params.put("df", dificultad);
        }
        int tamanio = Math.min(Math.max(size, 1), 100);
        int pagina = Math.max(page, 1);
        TypedQuery<Long> count = em.createQuery("SELECT COUNT(e.id)" + where, Long.class);
        TypedQuery<Ejercicio> query = em.createQuery("SELECT e" + where + " ORDER BY LOWER(e.nombre)", Ejercicio.class)
                .setFirstResult((pagina - 1) * tamanio).setMaxResults(tamanio);
        params.forEach((k, v) -> {
            count.setParameter(k, v);
            query.setParameter(k, v);
        });
        List<EjercicioDTO.Respuesta> filas = query.getResultList().stream().map(e -> ejercicioDto(e, alcance)).toList();
        return new PageImpl<>(filas, PageRequest.of(pagina - 1, tamanio), count.getSingleResult());
    }

    @Transactional
    public EjercicioDTO.Respuesta crearEjercicio(EjercicioDTO.Command command) {
        Alcance alcance = alcance();
        Ejercicio ejercicio = new Ejercicio();
        ejercicio.setCompany(alcance.company());
        ejercicio.setCreadoPor(alcance.user());
        aplicar(ejercicio, command);
        return ejercicioDto(ejercicioRepository.save(ejercicio), alcance);
    }

    @Transactional
    public EjercicioDTO.Respuesta editarEjercicio(String uuid, EjercicioDTO.Command command) {
        Alcance alcance = alcance();
        Ejercicio ejercicio = ejercicio(uuid, alcance);
        exigirEditable(ejercicio, alcance);
        aplicar(ejercicio, command);
        return ejercicioDto(ejercicioRepository.save(ejercicio), alcance);
    }

    @Transactional
    public void borrarEjercicio(String uuid) {
        Alcance alcance = alcance();
        Ejercicio ejercicio = ejercicio(uuid, alcance);
        exigirEditable(ejercicio, alcance);
        // * Baja logica: las rutinas que ya lo usan lo siguen mostrando; no se ofrece para rutinas nuevas
        ejercicio.setStatus(-1);
        ejercicio.setDateDeleted(new Timestamp(System.currentTimeMillis()));
        ejercicioRepository.save(ejercicio);
    }

    private void aplicar(Ejercicio e, EjercicioDTO.Command c) {
        e.setNombre(c.getNombre().trim());
        e.setDescripcion(limpio(c.getDescripcion()));
        e.setDisciplina(limpio(c.getDisciplina()));
        e.setGrupoMuscular(limpio(c.getGrupoMuscular()));
        e.setTipo(c.getTipo());
        e.setDificultad(c.getDificultad());
        e.setImagenUrl(limpio(c.getImagenUrl()));
        e.setVideoUrl(limpio(c.getVideoUrl()));
        e.setIndicaciones(limpio(c.getIndicaciones()));
    }

    private void exigirEditable(Ejercicio ejercicio, Alcance alcance) {
        if (!editable(ejercicio, alcance)) {
            throw new CustomException(HttpStatus.FORBIDDEN, "Solo quien cargó el ejercicio o un administrador puede modificarlo",
                    "EntrenamientoService | ejercicio ajeno: " + ejercicio.getUuid());
        }
    }

    private boolean editable(Ejercicio ejercicio, Alcance alcance) {
        return alcance.esAdmin()
                || (ejercicio.getCreadoPor() != null && ejercicio.getCreadoPor().getId().equals(alcance.user().getId()));
    }

    // ================================================================ Rutinas

    @Transactional(readOnly = true)
    public Page<RutinaDTO.Fila> listarRutinas(RutinaDTO.PaginateRequest request) {
        Alcance alcance = alcance();
        StringBuilder where = new StringBuilder(
                " FROM Rutina r LEFT JOIN r.actividad a LEFT JOIN r.profesor p LEFT JOIN p.user pu"
                        + " WHERE r.company.id = :c AND r.status = 0");
        Map<String, Object> params = new HashMap<>();
        params.put("c", alcance.company().getId());
        if (!alcance.esAdmin()) {
            where.append(" AND p.id = :profesor");
            params.put("profesor", alcance.profesor().getId());
        }
        if (tieneTexto(request.getSearch())) {
            where.append(" AND (LOWER(r.nombre) LIKE :s OR LOWER(r.disciplina) LIKE :s)");
            params.put("s", like(request.getSearch()));
        }
        if (request.getEstado() != null) {
            where.append(" AND r.estado = :e");
            params.put("e", request.getEstado());
        }
        if (tieneTexto(request.getMemberUuid())) {
            where.append(" AND EXISTS (SELECT 1 FROM RutinaAsignacion ra WHERE ra.rutina = r AND ra.status = 0 AND ra.member.uuid = :m)");
            params.put("m", request.getMemberUuid());
        }
        TypedQuery<Long> count = em.createQuery("SELECT COUNT(r.id)" + where, Long.class);
        TypedQuery<Object[]> query = em.createQuery(
                        "SELECT r.id, r.uuid, r.nombre, r.disciplina, a.nombre, pu.nombre, pu.apellido, r.estado, r.fechaInicio, r.fechaFin, r.dateUpdated"
                                + where + " ORDER BY r.dateUpdated DESC, r.id DESC", Object[].class)
                .setFirstResult((request.getPage() - 1) * request.getSize())
                .setMaxResults(request.getSize());
        params.forEach((k, v) -> {
            count.setParameter(k, v);
            query.setParameter(k, v);
        });
        List<Object[]> filas = query.getResultList();
        List<Long> ids = filas.stream().map(f -> (Long) f[0]).toList();
        Map<Long, Long> dias = contar("SELECT d.rutina.id, COUNT(d.id) FROM RutinaDia d WHERE d.rutina.id IN :ids AND d.status = 0 GROUP BY d.rutina.id", ids);
        Map<Long, Long> items = contar("SELECT i.dia.rutina.id, COUNT(i.id) FROM RutinaItem i WHERE i.dia.rutina.id IN :ids AND i.status = 0 GROUP BY i.dia.rutina.id", ids);
        Map<Long, Long> activas = contar("SELECT ra.rutina.id, COUNT(ra.id) FROM RutinaAsignacion ra WHERE ra.rutina.id IN :ids AND ra.status = 0"
                + " AND ra.estado = com.blue.open.Code.Entity.Enums.AsignacionRutinaEstadoEnum.ACTIVA GROUP BY ra.rutina.id", ids);

        List<RutinaDTO.Fila> resultado = new ArrayList<>();
        for (Object[] f : filas) {
            RutinaDTO.Fila fila = new RutinaDTO.Fila();
            Long id = (Long) f[0];
            fila.setUuid((String) f[1]);
            fila.setNombre((String) f[2]);
            fila.setDisciplina((String) f[3]);
            fila.setActividad((String) f[4]);
            fila.setProfesor(nombre(f[5], f[6]));
            fila.setEstado((RutinaEstadoEnum) f[7]);
            fila.setFechaInicio((LocalDate) f[8]);
            fila.setFechaFin((LocalDate) f[9]);
            fila.setActualizada((Timestamp) f[10]);
            fila.setDias(dias.getOrDefault(id, 0L).intValue());
            fila.setEjercicios(items.getOrDefault(id, 0L).intValue());
            fila.setAsignacionesActivas(activas.getOrDefault(id, 0L).intValue());
            resultado.add(fila);
        }
        return new PageImpl<>(resultado, PageRequest.of(request.getPage() - 1, request.getSize()), count.getSingleResult());
    }

    @Transactional(readOnly = true)
    public RutinaDTO.Detalle obtenerRutina(String uuid) {
        Alcance alcance = alcance();
        Rutina rutina = rutina(uuid, alcance);
        RutinaDTO.Detalle detalle = detalle(rutina, alcance.esAdmin() || rutina.getEstado() == RutinaEstadoEnum.ACTIVA);
        detalle.setEditable(rutina.getEstado() == RutinaEstadoEnum.ACTIVA);
        detalle.setAsignaciones(asignaciones(
                em.createQuery("SELECT ra FROM RutinaAsignacion ra JOIN FETCH ra.member m LEFT JOIN FETCH m.user"
                                + " WHERE ra.rutina.id = :r AND ra.status = 0 ORDER BY ra.estado, ra.fechaAsignacion DESC", RutinaAsignacion.class)
                        .setParameter("r", rutina.getId()).getResultList()));
        detalle.setHistorial(em.createQuery("SELECT h FROM RutinaHistorial h LEFT JOIN FETCH h.user WHERE h.rutina.id = :r ORDER BY h.fecha DESC, h.id DESC",
                        RutinaHistorial.class)
                .setParameter("r", rutina.getId()).getResultList().stream().map(h -> {
                    RutinaDTO.Historial dto = new RutinaDTO.Historial();
                    dto.setFecha(h.getFecha());
                    dto.setAccion(h.getAccion());
                    dto.setDetalle(h.getDetalle());
                    dto.setUsuario(h.getUser() != null ? nombre(h.getUser().getNombre(), h.getUser().getApellido()) : null);
                    return dto;
                }).toList());
        return detalle;
    }

    @Transactional
    public RutinaDTO.Detalle crearRutina(RutinaDTO.Command command) {
        Alcance alcance = alcance();
        Rutina rutina = new Rutina();
        rutina.setCompany(alcance.company());
        rutina.setCreadaPor(alcance.user());
        rutina.setEstado(RutinaEstadoEnum.ACTIVA);
        aplicar(rutina, command, alcance);
        rutina = rutinaRepository.save(rutina);
        int ejercicios = guardarDias(rutina, command.getDias(), alcance);
        historial(rutina, alcance, "CREADA", resumen(command.getDias().size(), ejercicios));
        if (command.getMemberUuids() != null && !command.getMemberUuids().isEmpty()) {
            RutinaDTO.AsignarCommand asignar = new RutinaDTO.AsignarCommand();
            asignar.setMemberUuids(command.getMemberUuids());
            asignar.setFechaInicio(command.getFechaInicio());
            asignar.setFechaFin(command.getFechaFin());
            asignar(rutina, asignar, alcance);
        }
        return obtenerRutina(rutina.getUuid());
    }

    @Transactional
    public RutinaDTO.Detalle editarRutina(String uuid, RutinaDTO.Command command) {
        Alcance alcance = alcance();
        Rutina rutina = rutinaActiva(uuid, alcance);
        aplicar(rutina, command, alcance);
        rutinaRepository.save(rutina);
        darDeBajaContenido(rutina);
        int ejercicios = guardarDias(rutina, command.getDias(), alcance);
        historial(rutina, alcance, "EDITADA", resumen(command.getDias().size(), ejercicios));
        return obtenerRutina(uuid);
    }

    /**
     * Reemplaza la rutina por una nueva version: la anterior queda REEMPLAZADA (con su historial) y los
     * socios que la tenian activa pasan a la nueva.
     */
    @Transactional
    public RutinaDTO.Detalle reemplazarRutina(String uuid, RutinaDTO.Command command) {
        Alcance alcance = alcance();
        Rutina anterior = rutinaActiva(uuid, alcance);
        Rutina nueva = new Rutina();
        nueva.setCompany(alcance.company());
        nueva.setCreadaPor(alcance.user());
        nueva.setEstado(RutinaEstadoEnum.ACTIVA);
        nueva.setReemplazaA(anterior);
        aplicar(nueva, command, alcance);
        nueva = rutinaRepository.save(nueva);
        int ejercicios = guardarDias(nueva, command.getDias(), alcance);

        List<RutinaAsignacion> activas = asignacionesActivas(anterior.getId());
        LocalDate hoy = LocalDate.now();
        for (RutinaAsignacion vieja : activas) {
            vieja.setEstado(AsignacionRutinaEstadoEnum.FINALIZADA);
            vieja.setFechaFinalizacion(hoy);
            RutinaAsignacion copia = new RutinaAsignacion();
            copia.setCompany(alcance.company());
            copia.setRutina(nueva);
            copia.setMember(vieja.getMember());
            copia.setAsignadaPor(alcance.user());
            copia.setFechaAsignacion(hoy);
            copia.setFechaInicio(command.getFechaInicio() != null ? command.getFechaInicio() : hoy);
            copia.setFechaFin(command.getFechaFin());
            copia.setEstado(AsignacionRutinaEstadoEnum.ACTIVA);
            asignacionRepository.save(copia);
        }
        asignacionRepository.saveAll(activas);
        anterior.setEstado(RutinaEstadoEnum.REEMPLAZADA);
        rutinaRepository.save(anterior);

        historial(anterior, alcance, "REEMPLAZADA", "Reemplazada por \"" + nueva.getNombre() + "\"");
        historial(nueva, alcance, "CREADA", "Reemplaza a \"" + anterior.getNombre() + "\" · "
                + resumen(command.getDias().size(), ejercicios)
                + (activas.isEmpty() ? "" : " · " + activas.size() + " socio(s) pasaron a esta rutina"));
        return obtenerRutina(nueva.getUuid());
    }

    @Transactional
    public RutinaDTO.Detalle archivarRutina(String uuid) {
        Alcance alcance = alcance();
        Rutina rutina = rutinaActiva(uuid, alcance);
        List<RutinaAsignacion> activas = asignacionesActivas(rutina.getId());
        activas.forEach(a -> {
            a.setEstado(AsignacionRutinaEstadoEnum.FINALIZADA);
            a.setFechaFinalizacion(LocalDate.now());
        });
        asignacionRepository.saveAll(activas);
        rutina.setEstado(RutinaEstadoEnum.ARCHIVADA);
        rutinaRepository.save(rutina);
        historial(rutina, alcance, "ARCHIVADA", activas.isEmpty() ? null : activas.size() + " asignación(es) finalizada(s)");
        return obtenerRutina(uuid);
    }

    @Transactional
    public RutinaDTO.Detalle asignar(String uuid, RutinaDTO.AsignarCommand command) {
        Alcance alcance = alcance();
        asignar(rutinaActiva(uuid, alcance), command, alcance);
        return obtenerRutina(uuid);
    }

    private void asignar(Rutina rutina, RutinaDTO.AsignarCommand command, Alcance alcance) {
        validarFechas(command.getFechaInicio(), command.getFechaFin());
        Set<Long> yaActivos = asignacionesActivas(rutina.getId()).stream()
                .map(a -> a.getMember().getId()).collect(Collectors.toSet());
        List<String> nombres = new ArrayList<>();
        LocalDate hoy = LocalDate.now();
        for (String memberUuid : new LinkedHashSet<>(command.getMemberUuids())) {
            Member member = memberAuthService.resolveMemberByUuid(memberUuid, alcance.company().getId());
            exigirAlumno(member, alcance);
            if (yaActivos.contains(member.getId())) {
                continue;
            }
            RutinaAsignacion asignacion = new RutinaAsignacion();
            asignacion.setCompany(alcance.company());
            asignacion.setRutina(rutina);
            asignacion.setMember(member);
            asignacion.setAsignadaPor(alcance.user());
            asignacion.setFechaAsignacion(hoy);
            asignacion.setFechaInicio(command.getFechaInicio() != null ? command.getFechaInicio() : hoy);
            asignacion.setFechaFin(command.getFechaFin());
            asignacion.setObservaciones(limpio(command.getObservaciones()));
            asignacion.setEstado(AsignacionRutinaEstadoEnum.ACTIVA);
            asignacionRepository.save(asignacion);
            nombres.add(nombreSocio(member));
        }
        if (!nombres.isEmpty()) {
            historial(rutina, alcance, "ASIGNADA", recortar(String.join(", ", nombres), 500));
        }
    }

    @Transactional
    public RutinaDTO.Detalle finalizarAsignacion(String asignacionUuid) {
        Alcance alcance = alcance();
        RutinaAsignacion asignacion = asignacion(asignacionUuid, alcance);
        if (asignacion.getEstado() == AsignacionRutinaEstadoEnum.ACTIVA) {
            asignacion.setEstado(AsignacionRutinaEstadoEnum.FINALIZADA);
            asignacion.setFechaFinalizacion(LocalDate.now());
            asignacionRepository.save(asignacion);
            historial(asignacion.getRutina(), alcance, "ASIGNACION_FINALIZADA", nombreSocio(asignacion.getMember()));
        }
        return obtenerRutina(asignacion.getRutina().getUuid());
    }

    /** Registra una sesion: que ejercicios hizo, esfuerzo y observaciones. */
    @Transactional
    public RutinaDTO.Detalle registrarSeguimiento(String asignacionUuid, RutinaDTO.SeguimientoCommand command) {
        Alcance alcance = alcance();
        RutinaAsignacion asignacion = asignacion(asignacionUuid, alcance);
        if (asignacion.getEstado() != AsignacionRutinaEstadoEnum.ACTIVA) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "La rutina ya no está activa para este socio",
                    "EntrenamientoService | asignacion finalizada: " + asignacionUuid);
        }
        if (command.getFecha().isAfter(LocalDate.now())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "No se puede registrar una sesión futura",
                    "EntrenamientoService | fecha futura");
        }
        RutinaSeguimiento seguimiento = new RutinaSeguimiento();
        seguimiento.setAsignacion(asignacion);
        seguimiento.setFecha(command.getFecha());
        seguimiento.setEsfuerzo(command.getEsfuerzo());
        seguimiento.setObservaciones(limpio(command.getObservaciones()));
        seguimiento.setRegistradoPor(alcance.user());
        if (command.getItemUuids() != null && !command.getItemUuids().isEmpty()) {
            List<RutinaItem> items = em.createQuery("SELECT i FROM RutinaItem i WHERE i.uuid IN :u AND i.dia.rutina.id = :r AND i.status = 0", RutinaItem.class)
                    .setParameter("u", new HashSet<>(command.getItemUuids()))
                    .setParameter("r", asignacion.getRutina().getId())
                    .getResultList();
            if (items.size() != new HashSet<>(command.getItemUuids()).size()) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "Algún ejercicio no pertenece a la rutina",
                        "EntrenamientoService | items invalidos");
            }
            seguimiento.getItemsRealizados().addAll(items);
        }
        seguimientoRepository.save(seguimiento);
        return obtenerRutina(asignacion.getRutina().getUuid());
    }

    /** Socios a los que se puede asignar: ADMIN todos los activos; profesor, los de sus actividades. */
    @Transactional(readOnly = true)
    public List<RutinaDTO.Alumno> alumnos(String search) {
        Alcance alcance = alcance();
        StringBuilder jpql = new StringBuilder(
                "SELECT m.uuid, u.nombre, u.apellido, u.dni, a.nombre FROM InscripcionActividad i JOIN i.member m JOIN m.user u JOIN i.actividad a"
                        + " WHERE i.company.id = :c AND i.status = 0 AND m.status = 0");
        Map<String, Object> params = new HashMap<>();
        params.put("c", alcance.company().getId());
        if (!alcance.esAdmin()) {
            if (alcance.actividades().isEmpty()) {
                return List.of();
            }
            jpql.append(" AND a.id IN :acts");
            params.put("acts", alcance.actividades());
        }
        if (tieneTexto(search)) {
            jpql.append(" AND (LOWER(CONCAT(u.nombre, ' ', u.apellido)) LIKE :s OR u.dni LIKE :s)");
            params.put("s", like(search));
        }
        jpql.append(" ORDER BY u.apellido, u.nombre");
        TypedQuery<Object[]> query = em.createQuery(jpql.toString(), Object[].class);
        params.forEach(query::setParameter);

        Map<String, RutinaDTO.Alumno> porMember = new LinkedHashMap<>();
        for (Object[] f : query.getResultList()) {
            RutinaDTO.Alumno alumno = porMember.computeIfAbsent((String) f[0], k -> {
                RutinaDTO.Alumno nuevo = new RutinaDTO.Alumno();
                nuevo.setMemberUuid(k);
                nuevo.setNombre((String) f[1]);
                nuevo.setApellido((String) f[2]);
                nuevo.setDni((String) f[3]);
                return nuevo;
            });
            alumno.setActividades(alumno.getActividades() == null ? (String) f[4] : alumno.getActividades() + ", " + f[4]);
            if (porMember.size() >= MAX_ALUMNOS) {
                break;
            }
        }
        return new ArrayList<>(porMember.values());
    }

    // ================================================================ Portal del socio

    /** Rutinas del socio actual o de un familiar de su grupo: activas primero, despues el historial. */
    @Transactional(readOnly = true)
    public List<RutinaDTO.Portal> rutinasDelSocio(String memberUuid) {
        authService.valid(List.of(PermissionEnum.PORTAL_TRAINING_VIEW));
        Company company = authService.getCurrentCompanyOrThrow();
        Member member = tieneTexto(memberUuid)
                ? memberAuthService.resolveMemberOfFamilyOrThrow(memberUuid, company.getId())
                : memberAuthService.getCurrentMemberOrThrow(company.getId());
        List<RutinaAsignacion> asignaciones = em.createQuery(
                        "SELECT ra FROM RutinaAsignacion ra JOIN FETCH ra.rutina r JOIN FETCH ra.member m LEFT JOIN FETCH m.user"
                                + " WHERE ra.company.id = :c AND ra.member.id = :m AND ra.status = 0 AND r.status = 0"
                                + " ORDER BY ra.estado, ra.fechaAsignacion DESC", RutinaAsignacion.class)
                .setParameter("c", company.getId())
                .setParameter("m", member.getId())
                .getResultList();
        List<RutinaDTO.Asignacion> dtos = asignaciones(asignaciones);
        List<RutinaDTO.Portal> resultado = new ArrayList<>();
        for (int i = 0; i < asignaciones.size(); i++) {
            RutinaDTO.Portal portal = new RutinaDTO.Portal();
            portal.setAsignacion(dtos.get(i));
            portal.setRutina(detalle(asignaciones.get(i).getRutina(), false));
            resultado.add(portal);
        }
        return resultado;
    }

    // ================================================================ Armado de respuestas

    private RutinaDTO.Detalle detalle(Rutina rutina, boolean incluirInactivos) {
        RutinaDTO.Detalle dto = new RutinaDTO.Detalle();
        dto.setUuid(rutina.getUuid());
        dto.setNombre(rutina.getNombre());
        dto.setObjetivo(rutina.getObjetivo());
        dto.setDisciplina(rutina.getDisciplina());
        if (rutina.getActividad() != null) {
            dto.setActividadUuid(rutina.getActividad().getUuid());
            dto.setActividad(rutina.getActividad().getNombre());
        }
        if (rutina.getProfesor() != null) {
            dto.setProfesorUuid(rutina.getProfesor().getUuid());
            User u = rutina.getProfesor().getUser();
            dto.setProfesor(u != null ? nombre(u.getNombre(), u.getApellido()) : null);
        }
        dto.setEstado(rutina.getEstado());
        dto.setFechaInicio(rutina.getFechaInicio());
        dto.setFechaFin(rutina.getFechaFin());
        dto.setObservaciones(rutina.getObservaciones());
        if (rutina.getReemplazaA() != null) {
            dto.setReemplazaAUuid(rutina.getReemplazaA().getUuid());
            dto.setReemplazaANombre(rutina.getReemplazaA().getNombre());
        }

        List<RutinaDia> dias = em.createQuery("SELECT d FROM RutinaDia d WHERE d.rutina.id = :r AND d.status = 0 ORDER BY d.orden", RutinaDia.class)
                .setParameter("r", rutina.getId()).getResultList();
        Map<Long, RutinaDTO.Dia> porDia = new LinkedHashMap<>();
        for (RutinaDia d : dias) {
            RutinaDTO.Dia dia = new RutinaDTO.Dia();
            dia.setUuid(d.getUuid());
            dia.setOrden(d.getOrden());
            dia.setNombre(d.getNombre());
            porDia.put(d.getId(), dia);
        }
        if (!porDia.isEmpty()) {
            List<RutinaItem> items = em.createQuery("SELECT i FROM RutinaItem i JOIN FETCH i.ejercicio WHERE i.dia.id IN :d AND i.status = 0 ORDER BY i.orden", RutinaItem.class)
                    .setParameter("d", porDia.keySet()).getResultList();
            for (RutinaItem i : items) {
                RutinaDTO.Item item = new RutinaDTO.Item();
                item.setUuid(i.getUuid());
                item.setOrden(i.getOrden());
                item.setSeries(i.getSeries());
                item.setRepeticiones(i.getRepeticiones());
                item.setCarga(i.getCarga());
                item.setDescansoSegundos(i.getDescansoSegundos());
                item.setObservaciones(i.getObservaciones());
                Ejercicio e = i.getEjercicio();
                RutinaDTO.EjercicioResumen ej = new RutinaDTO.EjercicioResumen();
                ej.setUuid(e.getUuid());
                ej.setNombre(e.getNombre());
                ej.setTipo(e.getTipo());
                ej.setDificultad(e.getDificultad());
                ej.setGrupoMuscular(e.getGrupoMuscular());
                ej.setImagenUrl(e.getImagenUrl());
                ej.setVideoUrl(e.getVideoUrl());
                ej.setIndicaciones(e.getIndicaciones());
                item.setEjercicio(ej);
                porDia.get(i.getDia().getId()).getItems().add(item);
            }
        }
        dto.setDias(new ArrayList<>(porDia.values()));
        return dto;
    }

    private List<RutinaDTO.Asignacion> asignaciones(List<RutinaAsignacion> asignaciones) {
        if (asignaciones.isEmpty()) {
            return List.of();
        }
        Map<Long, List<RutinaSeguimiento>> seguimientos = em.createQuery(
                        "SELECT DISTINCT s FROM RutinaSeguimiento s LEFT JOIN FETCH s.itemsRealizados LEFT JOIN FETCH s.registradoPor"
                                + " WHERE s.asignacion.id IN :a AND s.status = 0", RutinaSeguimiento.class)
                .setParameter("a", asignaciones.stream().map(RutinaAsignacion::getId).toList())
                .getResultList().stream()
                .collect(Collectors.groupingBy(s -> s.getAsignacion().getId()));

        List<RutinaDTO.Asignacion> resultado = new ArrayList<>();
        for (RutinaAsignacion a : asignaciones) {
            RutinaDTO.Asignacion dto = new RutinaDTO.Asignacion();
            dto.setUuid(a.getUuid());
            dto.setRutinaUuid(a.getRutina().getUuid());
            dto.setRutinaNombre(a.getRutina().getNombre());
            dto.setMemberUuid(a.getMember().getUuid());
            dto.setSocio(nombreSocio(a.getMember()));
            dto.setDni(a.getMember().getUser() != null ? a.getMember().getUser().getDni() : null);
            dto.setEstado(a.getEstado());
            dto.setFechaAsignacion(a.getFechaAsignacion());
            dto.setFechaInicio(a.getFechaInicio());
            dto.setFechaFin(a.getFechaFin());
            dto.setFechaFinalizacion(a.getFechaFinalizacion());
            dto.setObservaciones(a.getObservaciones());
            User asignadaPor = a.getAsignadaPor();
            dto.setAsignadaPor(asignadaPor != null ? nombre(asignadaPor.getNombre(), asignadaPor.getApellido()) : null);
            List<RutinaSeguimiento> sesiones = new ArrayList<>(seguimientos.getOrDefault(a.getId(), List.of()));
            sesiones.sort((x, y) -> y.getFecha().compareTo(x.getFecha()));
            dto.setSesiones(sesiones.size());
            dto.setUltimaSesion(sesiones.isEmpty() ? null : sesiones.get(0).getFecha());
            for (RutinaSeguimiento s : sesiones) {
                RutinaDTO.Seguimiento seg = new RutinaDTO.Seguimiento();
                seg.setUuid(s.getUuid());
                seg.setFecha(s.getFecha());
                seg.setEsfuerzo(s.getEsfuerzo());
                seg.setObservaciones(s.getObservaciones());
                seg.setRegistradoPor(s.getRegistradoPor() != null
                        ? nombre(s.getRegistradoPor().getNombre(), s.getRegistradoPor().getApellido()) : null);
                seg.setItemUuids(s.getItemsRealizados().stream().map(RutinaItem::getUuid).toList());
                dto.getSeguimientos().add(seg);
            }
            resultado.add(dto);
        }
        return resultado;
    }

    private EjercicioDTO.Respuesta ejercicioDto(Ejercicio e, Alcance alcance) {
        EjercicioDTO.Respuesta dto = new EjercicioDTO.Respuesta();
        dto.setUuid(e.getUuid());
        dto.setNombre(e.getNombre());
        dto.setDescripcion(e.getDescripcion());
        dto.setDisciplina(e.getDisciplina());
        dto.setGrupoMuscular(e.getGrupoMuscular());
        dto.setTipo(e.getTipo());
        dto.setDificultad(e.getDificultad());
        dto.setImagenUrl(e.getImagenUrl());
        dto.setVideoUrl(e.getVideoUrl());
        dto.setIndicaciones(e.getIndicaciones());
        User u = e.getCreadoPor();
        dto.setCreadoPor(u != null ? nombre(u.getNombre(), u.getApellido()) : null);
        dto.setEditable(editable(e, alcance));
        return dto;
    }

    // ================================================================ Escritura de rutinas

    private void aplicar(Rutina rutina, RutinaDTO.Command c, Alcance alcance) {
        validarFechas(c.getFechaInicio(), c.getFechaFin());
        rutina.setNombre(c.getNombre().trim());
        rutina.setObjetivo(limpio(c.getObjetivo()));
        rutina.setDisciplina(limpio(c.getDisciplina()));
        rutina.setFechaInicio(c.getFechaInicio());
        rutina.setFechaFin(c.getFechaFin());
        rutina.setObservaciones(limpio(c.getObservaciones()));

        Actividad actividad = null;
        if (tieneTexto(c.getActividadUuid())) {
            actividad = actividadRepository.findByUuidAndCompany_Id(c.getActividadUuid(), alcance.company().getId())
                    .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Actividad no encontrada",
                            "EntrenamientoService | actividadUuid: " + c.getActividadUuid()));
            if (!alcance.esAdmin() && !alcance.actividades().contains(actividad.getId())) {
                throw sinAcceso("actividad ajena: " + c.getActividadUuid());
            }
        }
        rutina.setActividad(actividad);

        if (!alcance.esAdmin()) {
            rutina.setProfesor(alcance.profesor());
        } else if (tieneTexto(c.getProfesorUuid())) {
            rutina.setProfesor(profesorRepository.findByUuidAndCompanyId(c.getProfesorUuid(), alcance.company().getId())
                    .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Profesor no encontrado",
                            "EntrenamientoService | profesorUuid: " + c.getProfesorUuid())));
        } else {
            rutina.setProfesor(null);
        }
    }

    private int guardarDias(Rutina rutina, List<RutinaDTO.DiaCommand> dias, Alcance alcance) {
        Set<String> uuids = dias.stream().flatMap(d -> d.getItems().stream()).map(RutinaDTO.ItemCommand::getEjercicioUuid)
                .collect(Collectors.toSet());
        Map<String, Ejercicio> ejercicios = em.createQuery("SELECT e FROM Ejercicio e WHERE e.uuid IN :u AND e.company.id = :c AND e.status = 0", Ejercicio.class)
                .setParameter("u", uuids)
                .setParameter("c", alcance.company().getId())
                .getResultList().stream()
                .collect(Collectors.toMap(Ejercicio::getUuid, e -> e));
        if (ejercicios.size() != uuids.size()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Algún ejercicio no existe en la biblioteca del gimnasio",
                    "EntrenamientoService | ejercicios faltantes");
        }
        int total = 0;
        int ordenDia = 1;
        for (RutinaDTO.DiaCommand d : dias) {
            RutinaDia dia = new RutinaDia();
            dia.setRutina(rutina);
            dia.setOrden(ordenDia++);
            dia.setNombre(d.getNombre().trim());
            dia = diaRepository.save(dia);
            int ordenItem = 1;
            for (RutinaDTO.ItemCommand i : d.getItems()) {
                RutinaItem item = new RutinaItem();
                item.setDia(dia);
                item.setEjercicio(ejercicios.get(i.getEjercicioUuid()));
                item.setOrden(ordenItem++);
                item.setSeries(i.getSeries());
                item.setRepeticiones(limpio(i.getRepeticiones()));
                item.setCarga(limpio(i.getCarga()));
                item.setDescansoSegundos(i.getDescansoSegundos());
                item.setObservaciones(limpio(i.getObservaciones()));
                itemRepository.save(item);
                total++;
            }
        }
        return total;
    }

    private void darDeBajaContenido(Rutina rutina) {
        Timestamp ahora = new Timestamp(System.currentTimeMillis());
        em.createQuery("UPDATE RutinaItem i SET i.status = -1, i.dateDeleted = :t WHERE i.status = 0 AND i.dia.id IN"
                        + " (SELECT d.id FROM RutinaDia d WHERE d.rutina.id = :r)")
                .setParameter("t", ahora).setParameter("r", rutina.getId()).executeUpdate();
        em.createQuery("UPDATE RutinaDia d SET d.status = -1, d.dateDeleted = :t WHERE d.status = 0 AND d.rutina.id = :r")
                .setParameter("t", ahora).setParameter("r", rutina.getId()).executeUpdate();
    }

    private void historial(Rutina rutina, Alcance alcance, String accion, String detalle) {
        RutinaHistorial h = new RutinaHistorial();
        h.setRutina(rutina);
        h.setUser(alcance.user());
        h.setFecha(new Timestamp(System.currentTimeMillis()));
        h.setAccion(accion);
        h.setDetalle(detalle);
        historialRepository.save(h);
    }

    // ================================================================ Alcance y busquedas

    private Alcance alcance() {
        authService.valid(List.of(PermissionEnum.TRAINING_MANAGE));
        Company company = authService.getCurrentCompanyOrThrow();
        User user = authService.getActualOrThrow();
        if (authService.validBoolean(List.of(BaseRoleEnum.ADMIN, BaseRoleEnum.SYSADMIN))) {
            return new Alcance(company, user, null, Set.of());
        }
        Profesor profesor = profesorAuthService.requirePortalEligible(profesorAuthService.getCurrentProfesorOrThrow(company.getId()));
        Set<Long> actividades = new HashSet<>(actividadProfesorRepository.findActividadIdsByProfesorId(profesor.getId()));
        actividades.retainAll(new HashSet<>(actividadRepository.findIdsActivasByCompanyId(company.getId())));
        return new Alcance(company, user, profesor, actividades);
    }

    private Ejercicio ejercicio(String uuid, Alcance alcance) {
        return em.createQuery("SELECT e FROM Ejercicio e LEFT JOIN FETCH e.creadoPor WHERE e.uuid = :u AND e.company.id = :c AND e.status = 0", Ejercicio.class)
                .setParameter("u", uuid).setParameter("c", alcance.company().getId())
                .getResultStream().findFirst()
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Ejercicio no encontrado",
                        "EntrenamientoService | ejercicioUuid: " + uuid));
    }

    private Rutina rutina(String uuid, Alcance alcance) {
        Rutina rutina = em.createQuery("SELECT r FROM Rutina r LEFT JOIN FETCH r.profesor p LEFT JOIN FETCH p.user LEFT JOIN FETCH r.actividad"
                                + " WHERE r.uuid = :u AND r.company.id = :c AND r.status = 0", Rutina.class)
                .setParameter("u", uuid).setParameter("c", alcance.company().getId())
                .getResultStream().findFirst()
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Rutina no encontrada",
                        "EntrenamientoService | rutinaUuid: " + uuid));
        if (!alcance.esAdmin() && (rutina.getProfesor() == null || !rutina.getProfesor().getId().equals(alcance.profesor().getId()))) {
            throw sinAcceso("rutina ajena: " + uuid);
        }
        return rutina;
    }

    private Rutina rutinaActiva(String uuid, Alcance alcance) {
        Rutina rutina = rutina(uuid, alcance);
        if (rutina.getEstado() != RutinaEstadoEnum.ACTIVA) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "La rutina ya no está activa",
                    "EntrenamientoService | rutina " + rutina.getEstado() + ": " + uuid);
        }
        return rutina;
    }

    private RutinaAsignacion asignacion(String uuid, Alcance alcance) {
        RutinaAsignacion asignacion = em.createQuery("SELECT ra FROM RutinaAsignacion ra JOIN FETCH ra.rutina JOIN FETCH ra.member m LEFT JOIN FETCH m.user"
                                + " WHERE ra.uuid = :u AND ra.company.id = :c AND ra.status = 0", RutinaAsignacion.class)
                .setParameter("u", uuid).setParameter("c", alcance.company().getId())
                .getResultStream().findFirst()
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Asignación no encontrada",
                        "EntrenamientoService | asignacionUuid: " + uuid));
        // ! Valida que la rutina sea del profesor
        rutina(asignacion.getRutina().getUuid(), alcance);
        return asignacion;
    }

    private List<RutinaAsignacion> asignacionesActivas(Long rutinaId) {
        return em.createQuery("SELECT ra FROM RutinaAsignacion ra JOIN FETCH ra.member WHERE ra.rutina.id = :r AND ra.status = 0"
                        + " AND ra.estado = com.blue.open.Code.Entity.Enums.AsignacionRutinaEstadoEnum.ACTIVA", RutinaAsignacion.class)
                .setParameter("r", rutinaId).getResultList();
    }

    private void exigirAlumno(Member member, Alcance alcance) {
        if (alcance.esAdmin()) {
            return;
        }
        Long cantidad = alcance.actividades().isEmpty() ? 0L : em.createQuery(
                        "SELECT COUNT(i.id) FROM InscripcionActividad i WHERE i.member.id = :m AND i.actividad.id IN :a AND i.status = 0", Long.class)
                .setParameter("m", member.getId()).setParameter("a", alcance.actividades()).getSingleResult();
        if (cantidad == 0) {
            throw new CustomException(HttpStatus.FORBIDDEN, nombreSocio(member) + " no es alumno de tus actividades",
                    "EntrenamientoService | member ajeno: " + member.getUuid());
        }
    }

    private Map<Long, Long> contar(String jpql, Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> mapa = new HashMap<>();
        for (Object[] f : em.createQuery(jpql, Object[].class).setParameter("ids", ids).getResultList()) {
            mapa.put((Long) f[0], (Long) f[1]);
        }
        return mapa;
    }

    // ================================================================ Utilidades

    private static void validarFechas(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "La fecha de finalización no puede ser anterior al inicio",
                    "EntrenamientoService | fechas invertidas");
        }
    }

    static String resumen(int dias, int ejercicios) {
        return dias + (dias == 1 ? " día" : " días") + ", " + ejercicios + (ejercicios == 1 ? " ejercicio" : " ejercicios");
    }

    private static CustomException sinAcceso(String detalle) {
        return new CustomException(HttpStatus.FORBIDDEN, "Esta rutina no está a tu cargo",
                "EntrenamientoService | sin acceso | " + detalle);
    }

    private static String nombreSocio(Member member) {
        User u = member.getUser();
        return u == null ? member.getUuid() : nombre(u.getNombre(), u.getApellido());
    }

    private static String nombre(Object nombre, Object apellido) {
        String texto = (Objects.toString(nombre, "") + " " + Objects.toString(apellido, "")).trim();
        return texto.isEmpty() ? null : texto;
    }

    private static String like(String texto) {
        return "%" + texto.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static String limpio(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private static String recortar(String texto, int max) {
        return texto.length() <= max ? texto : texto.substring(0, max - 1) + "…";
    }
}
