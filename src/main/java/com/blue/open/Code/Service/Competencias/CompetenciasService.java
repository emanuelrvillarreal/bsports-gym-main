package com.blue.open.Code.Service.Competencias;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.CompetenciasDTO;
import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.Categoria;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.EquipoDeportivo;
import com.blue.open.Code.Entity.Db.EquipoJugador;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.PartidoEstadistica;
import com.blue.open.Code.Entity.Db.Profesor;
import com.blue.open.Code.Entity.Db.Torneo;
import com.blue.open.Code.Entity.Db.TorneoParticipante;
import com.blue.open.Code.Entity.Db.TorneoPartido;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.PartidoEstadoEnum;
import com.blue.open.Code.Repository.EquipoDeportivoRepository;
import com.blue.open.Code.Repository.EquipoJugadorRepository;
import com.blue.open.Code.Repository.PartidoEstadisticaRepository;
import com.blue.open.Code.Repository.TorneoParticipanteRepository;
import com.blue.open.Code.Repository.TorneoPartidoRepository;
import com.blue.open.Code.Repository.TorneoRepository;
import com.blue.open.Code.Service.MemberAuthService;
import com.blue.open.Code.Service.ProfesorAuthService;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Equipos y planteles deportivos, torneos, partidos y estadisticas, siempre del club del header.
 *
 * - ADMIN (TEAMS_MANAGE): gestiona todo.
 * - PROFESOR (TEAMS_VIEW_ASSIGNED): consulta los equipos donde es entrenador y los torneos donde juegan.
 * Los planteles no se borran: una baja o un pase cierran el periodo del jugador, asi queda el plantel
 * historico. La tabla de posiciones y las estadisticas se calculan con los partidos JUGADO.
 */
@Service
public class CompetenciasService {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private AuthService authService;

    @Autowired
    private ProfesorAuthService profesorAuthService;

    @Autowired
    private MemberAuthService memberAuthService;

    @Autowired
    private EquipoDeportivoRepository equipoRepository;

    @Autowired
    private EquipoJugadorRepository jugadorRepository;

    @Autowired
    private TorneoRepository torneoRepository;

    @Autowired
    private TorneoParticipanteRepository participanteRepository;

    @Autowired
    private TorneoPartidoRepository partidoRepository;

    @Autowired
    private PartidoEstadisticaRepository estadisticaRepository;

    // ================================================================ Opciones para formularios

    @Transactional(readOnly = true)
    public CompetenciasDTO.Opciones opciones() {
        Company company = admin();
        CompetenciasDTO.Opciones o = new CompetenciasDTO.Opciones();
        for (Object[] f : em.createQuery("SELECT p.uuid, u.nombre, u.apellido FROM Profesor p JOIN p.user u WHERE p.company.id = :c AND p.status = 0"
                + " ORDER BY u.apellido, u.nombre", Object[].class).setParameter("c", company.getId()).getResultList()) {
            o.getProfesores().add(new CompetenciasDTO.Opcion((String) f[0], nombre(f[1], f[2])));
        }
        for (Object[] f : em.createQuery("SELECT c.uuid, c.nombre FROM Categoria c WHERE c.company.id = :c AND c.status = 0 ORDER BY c.nombre", Object[].class)
                .setParameter("c", company.getId()).getResultList()) {
            o.getCategorias().add(new CompetenciasDTO.Opcion((String) f[0], (String) f[1]));
        }
        for (Object[] f : em.createQuery("SELECT a.uuid, a.nombre FROM Actividad a WHERE a.company.id = :c AND a.status = 0 ORDER BY a.nombre", Object[].class)
                .setParameter("c", company.getId()).getResultList()) {
            o.getActividades().add(new CompetenciasDTO.Opcion((String) f[0], (String) f[1]));
        }
        for (Object[] f : em.createQuery("SELECT e.uuid, e.nombre FROM EquipoDeportivo e WHERE e.company.id = :c AND e.status = 0 AND e.activo = true ORDER BY e.nombre", Object[].class)
                .setParameter("c", company.getId()).getResultList()) {
            o.getEquipos().add(new CompetenciasDTO.Opcion((String) f[0], (String) f[1]));
        }
        return o;
    }

    @Transactional(readOnly = true)
    public List<CompetenciasDTO.SocioOpcion> socios(String search) {
        Company company = admin();
        StringBuilder jpql = new StringBuilder("SELECT m.uuid, u.nombre, u.apellido, u.dni, c.nombre FROM Member m JOIN m.user u LEFT JOIN m.categoria c"
                + " WHERE m.company.id = :c AND m.status = 0");
        if (tieneTexto(search)) {
            jpql.append(" AND (LOWER(CONCAT(u.nombre, ' ', u.apellido)) LIKE :s OR u.dni LIKE :s)");
        }
        jpql.append(" ORDER BY u.apellido, u.nombre");
        var query = em.createQuery(jpql.toString(), Object[].class).setParameter("c", company.getId()).setMaxResults(100);
        if (tieneTexto(search)) query.setParameter("s", like(search));
        List<CompetenciasDTO.SocioOpcion> socios = new ArrayList<>();
        for (Object[] f : query.getResultList()) {
            CompetenciasDTO.SocioOpcion s = new CompetenciasDTO.SocioOpcion();
            s.setMemberUuid((String) f[0]);
            s.setNombre((String) f[1]);
            s.setApellido((String) f[2]);
            s.setDni((String) f[3]);
            s.setCategoria((String) f[4]);
            socios.add(s);
        }
        return socios;
    }

    // ================================================================ Equipos

    @Transactional(readOnly = true)
    public List<CompetenciasDTO.EquipoFila> listarEquipos(CompetenciasDTO.Filtro filtro) {
        Company company = admin();
        StringBuilder jpql = new StringBuilder("SELECT DISTINCT e FROM EquipoDeportivo e LEFT JOIN FETCH e.categoria LEFT JOIN FETCH e.entrenadores p"
                + " LEFT JOIN FETCH p.user WHERE e.company.id = :c AND e.status = 0");
        Map<String, Object> params = new HashMap<>();
        params.put("c", company.getId());
        if (!filtro.isIncluirInactivos()) jpql.append(" AND e.activo = true");
        if (tieneTexto(filtro.getSearch())) {
            jpql.append(" AND (LOWER(e.nombre) LIKE :s OR LOWER(e.temporada) LIKE :s)");
            params.put("s", like(filtro.getSearch()));
        }
        if (tieneTexto(filtro.getDisciplina())) {
            jpql.append(" AND LOWER(e.disciplina) LIKE :d");
            params.put("d", like(filtro.getDisciplina()));
        }
        var query = em.createQuery(jpql.toString(), EquipoDeportivo.class);
        params.forEach(query::setParameter);
        List<EquipoDeportivo> equipos = query.getResultList().stream()
                .sorted(Comparator.comparing(EquipoDeportivo::getNombre, String.CASE_INSENSITIVE_ORDER)).toList();
        return filasEquipos(equipos);
    }

    @Transactional(readOnly = true)
    public CompetenciasDTO.EquipoDetalle obtenerEquipo(String uuid) {
        Company company = admin();
        return detalleEquipo(equipo(uuid, company), true);
    }

    @Transactional
    public CompetenciasDTO.EquipoDetalle crearEquipo(CompetenciasDTO.EquipoCommand c) {
        Company company = admin();
        EquipoDeportivo e = new EquipoDeportivo();
        e.setCompany(company);
        aplicar(e, c, company);
        return detalleEquipo(equipoRepository.save(e), true);
    }

    @Transactional
    public CompetenciasDTO.EquipoDetalle editarEquipo(String uuid, CompetenciasDTO.EquipoCommand c) {
        Company company = admin();
        EquipoDeportivo e = equipo(uuid, company);
        aplicar(e, c, company);
        return detalleEquipo(equipoRepository.save(e), true);
    }

    @Transactional
    public CompetenciasDTO.EquipoDetalle agregarJugador(String equipoUuid, CompetenciasDTO.JugadorCommand c) {
        Company company = admin();
        EquipoDeportivo equipo = equipo(equipoUuid, company);
        Member member = memberAuthService.resolveMemberByUuid(c.getMemberUuid(), company.getId());
        boolean yaEsta = jugadoresActivos(equipo.getId()).stream().anyMatch(j -> j.getMember().getId().equals(member.getId()));
        if (yaEsta) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Ese socio ya está en el plantel", "CompetenciasService | jugador repetido");
        }
        validarDorsal(equipo.getId(), c.getDorsal(), null);
        EquipoJugador j = new EquipoJugador();
        j.setEquipo(equipo);
        j.setMember(member);
        j.setDorsal(c.getDorsal());
        j.setPosicion(limpio(c.getPosicion()));
        j.setCapitan(c.isCapitan());
        j.setFechaAlta(c.getFechaAlta() != null ? c.getFechaAlta() : LocalDate.now());
        jugadorRepository.save(j);
        return detalleEquipo(equipo, true);
    }

    @Transactional
    public CompetenciasDTO.EquipoDetalle editarJugador(String jugadorUuid, CompetenciasDTO.JugadorEdicionCommand c) {
        Company company = admin();
        EquipoJugador j = jugadorActivo(jugadorUuid, company);
        validarDorsal(j.getEquipo().getId(), c.getDorsal(), j.getId());
        j.setDorsal(c.getDorsal());
        j.setPosicion(limpio(c.getPosicion()));
        j.setCapitan(c.isCapitan());
        jugadorRepository.save(j);
        return detalleEquipo(j.getEquipo(), true);
    }

    @Transactional
    public CompetenciasDTO.EquipoDetalle bajaJugador(String jugadorUuid, CompetenciasDTO.BajaJugadorCommand c) {
        Company company = admin();
        EquipoJugador j = jugadorActivo(jugadorUuid, company);
        cerrar(j, c.getFecha(), tieneTexto(c.getMotivo()) ? c.getMotivo().trim() : "Baja del plantel");
        return detalleEquipo(j.getEquipo(), true);
    }

    /** Pase entre equipos: cierra el periodo en el origen y abre uno nuevo en el destino. */
    @Transactional
    public CompetenciasDTO.EquipoDetalle pase(String jugadorUuid, CompetenciasDTO.PaseCommand c) {
        Company company = admin();
        EquipoJugador origen = jugadorActivo(jugadorUuid, company);
        EquipoDeportivo destino = equipo(c.getEquipoDestinoUuid(), company);
        if (destino.getId().equals(origen.getEquipo().getId())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "El equipo de destino es el mismo", "CompetenciasService | pase al mismo equipo");
        }
        boolean yaEsta = jugadoresActivos(destino.getId()).stream().anyMatch(j -> j.getMember().getId().equals(origen.getMember().getId()));
        if (yaEsta) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "El jugador ya está en " + destino.getNombre(), "CompetenciasService | pase duplicado");
        }
        validarDorsal(destino.getId(), c.getDorsal(), null);
        LocalDate fecha = c.getFecha() != null ? c.getFecha() : LocalDate.now();
        cerrar(origen, fecha, "Pase a " + destino.getNombre());
        EquipoJugador nuevo = new EquipoJugador();
        nuevo.setEquipo(destino);
        nuevo.setMember(origen.getMember());
        nuevo.setDorsal(c.getDorsal());
        nuevo.setPosicion(tieneTexto(c.getPosicion()) ? c.getPosicion().trim() : origen.getPosicion());
        nuevo.setFechaAlta(fecha);
        jugadorRepository.save(nuevo);
        return detalleEquipo(origen.getEquipo(), true);
    }

    // ================================================================ Torneos

    @Transactional(readOnly = true)
    public List<CompetenciasDTO.TorneoFila> listarTorneos(CompetenciasDTO.Filtro filtro) {
        Company company = admin();
        StringBuilder jpql = new StringBuilder("SELECT t FROM Torneo t LEFT JOIN FETCH t.categoria WHERE t.company.id = :c AND t.status = 0");
        Map<String, Object> params = new HashMap<>();
        params.put("c", company.getId());
        if (!filtro.isIncluirInactivos()) jpql.append(" AND t.estado <> com.blue.open.Code.Entity.Enums.TorneoEstadoEnum.FINALIZADO");
        if (tieneTexto(filtro.getSearch())) {
            jpql.append(" AND (LOWER(t.nombre) LIKE :s OR LOWER(t.temporada) LIKE :s)");
            params.put("s", like(filtro.getSearch()));
        }
        if (tieneTexto(filtro.getDisciplina())) {
            jpql.append(" AND LOWER(t.disciplina) LIKE :d");
            params.put("d", like(filtro.getDisciplina()));
        }
        jpql.append(" ORDER BY t.fechaInicio DESC, t.id DESC");
        var query = em.createQuery(jpql.toString(), Torneo.class);
        params.forEach(query::setParameter);
        return filasTorneos(query.getResultList());
    }

    @Transactional(readOnly = true)
    public CompetenciasDTO.TorneoDetalle obtenerTorneo(String uuid) {
        return detalleTorneo(torneo(uuid, admin()), true);
    }

    @Transactional
    public CompetenciasDTO.TorneoDetalle crearTorneo(CompetenciasDTO.TorneoCommand c) {
        Company company = admin();
        Torneo t = new Torneo();
        t.setCompany(company);
        aplicar(t, c, company);
        return detalleTorneo(torneoRepository.save(t), true);
    }

    @Transactional
    public CompetenciasDTO.TorneoDetalle editarTorneo(String uuid, CompetenciasDTO.TorneoCommand c) {
        Company company = admin();
        Torneo t = torneo(uuid, company);
        aplicar(t, c, company);
        return detalleTorneo(torneoRepository.save(t), true);
    }

    @Transactional
    public CompetenciasDTO.TorneoDetalle agregarParticipante(String torneoUuid, CompetenciasDTO.ParticipanteCommand c) {
        Company company = admin();
        Torneo torneo = torneo(torneoUuid, company);
        TorneoParticipante p = new TorneoParticipante();
        p.setTorneo(torneo);
        List<TorneoParticipante> actuales = participantes(torneo.getId());
        if (tieneTexto(c.getEquipoUuid())) {
            EquipoDeportivo equipo = equipo(c.getEquipoUuid(), company);
            if (actuales.stream().anyMatch(a -> a.getEquipo() != null && a.getEquipo().getId().equals(equipo.getId()))) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "Ese equipo ya participa", "CompetenciasService | participante repetido");
            }
            p.setEquipo(equipo);
            p.setNombre(equipo.getNombre());
        } else if (tieneTexto(c.getNombre())) {
            String nombre = c.getNombre().trim();
            if (actuales.stream().anyMatch(a -> a.getNombre().equalsIgnoreCase(nombre))) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "Ya hay un participante con ese nombre", "CompetenciasService | nombre repetido");
            }
            p.setNombre(nombre);
        } else {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Elegí un equipo del gimnasio o escribí el nombre del rival", "CompetenciasService | participante vacio");
        }
        participanteRepository.save(p);
        return detalleTorneo(torneo, true);
    }

    @Transactional
    public CompetenciasDTO.TorneoDetalle quitarParticipante(String participanteUuid) {
        Company company = admin();
        TorneoParticipante p = em.createQuery("SELECT p FROM TorneoParticipante p JOIN FETCH p.torneo t WHERE p.uuid = :u AND t.company.id = :c AND p.status = 0",
                        TorneoParticipante.class)
                .setParameter("u", participanteUuid).setParameter("c", company.getId())
                .getResultStream().findFirst()
                .orElseThrow(() -> noEncontrado("Participante", participanteUuid));
        Long partidos = em.createQuery("SELECT COUNT(m.id) FROM TorneoPartido m WHERE m.status = 0 AND (m.local.id = :p OR m.visitante.id = :p)", Long.class)
                .setParameter("p", p.getId()).getSingleResult();
        if (partidos > 0) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Tiene partidos cargados: eliminá esos partidos primero", "CompetenciasService | participante con partidos");
        }
        bajaLogica(p);
        participanteRepository.save(p);
        return detalleTorneo(p.getTorneo(), true);
    }

    @Transactional
    public CompetenciasDTO.TorneoDetalle crearPartido(String torneoUuid, CompetenciasDTO.PartidoCommand c) {
        Company company = admin();
        Torneo torneo = torneo(torneoUuid, company);
        TorneoPartido m = new TorneoPartido();
        m.setTorneo(torneo);
        aplicar(m, c, torneo);
        partidoRepository.save(m);
        return detalleTorneo(torneo, true);
    }

    @Transactional
    public CompetenciasDTO.TorneoDetalle editarPartido(String partidoUuid, CompetenciasDTO.PartidoCommand c) {
        Company company = admin();
        TorneoPartido m = partido(partidoUuid, company);
        aplicar(m, c, m.getTorneo());
        partidoRepository.save(m);
        return detalleTorneo(m.getTorneo(), true);
    }

    @Transactional
    public CompetenciasDTO.TorneoDetalle eliminarPartido(String partidoUuid) {
        Company company = admin();
        TorneoPartido m = partido(partidoUuid, company);
        bajaLogica(m);
        partidoRepository.save(m);
        return detalleTorneo(m.getTorneo(), true);
    }

    /** Jugadores de los equipos del club que jugaron el partido, con lo que ya tengan cargado. */
    @Transactional(readOnly = true)
    public CompetenciasDTO.EstadisticasPartido estadisticas(String partidoUuid) {
        Company company = admin();
        TorneoPartido m = partido(partidoUuid, company);
        CompetenciasDTO.EstadisticasPartido dto = new CompetenciasDTO.EstadisticasPartido();
        dto.setPartido(partidoDto(m, Set.of()));
        Map<Long, PartidoEstadistica> cargadas = em.createQuery("SELECT s FROM PartidoEstadistica s WHERE s.partido.id = :p AND s.status = 0", PartidoEstadistica.class)
                .setParameter("p", m.getId()).getResultList().stream()
                .collect(Collectors.toMap(s -> s.getJugador().getId(), s -> s, (a, b) -> a));
        for (EquipoJugador j : convocables(m)) {
            CompetenciasDTO.EstadisticaJugador fila = new CompetenciasDTO.EstadisticaJugador();
            fila.setJugadorUuid(j.getUuid());
            fila.setNombre(nombreSocio(j.getMember()));
            fila.setEquipo(j.getEquipo().getNombre());
            fila.setDorsal(j.getDorsal());
            PartidoEstadistica s = cargadas.get(j.getId());
            if (s != null) {
                fila.setTantos(s.getTantos());
                fila.setAsistencias(s.getAsistencias());
                fila.setAmonestaciones(s.getAmonestaciones());
                fila.setExpulsiones(s.getExpulsiones());
                fila.setFigura(s.isFigura());
                fila.setObservaciones(s.getObservaciones());
            }
            dto.getJugadores().add(fila);
        }
        return dto;
    }

    /** Reemplaza las estadisticas del partido. Solo jugadores del club que estaban en el plantel ese dia. */
    @Transactional
    public CompetenciasDTO.EstadisticasPartido guardarEstadisticas(String partidoUuid, CompetenciasDTO.EstadisticasCommand c) {
        Company company = admin();
        TorneoPartido m = partido(partidoUuid, company);
        if (m.getEstado() != PartidoEstadoEnum.JUGADO) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Cargá el resultado y marcá el partido como jugado antes de las estadísticas",
                    "CompetenciasService | estadisticas de partido no jugado");
        }
        Map<String, EquipoJugador> validos = convocables(m).stream().collect(Collectors.toMap(EquipoJugador::getUuid, j -> j));
        em.createQuery("DELETE FROM PartidoEstadistica s WHERE s.partido.id = :p").setParameter("p", m.getId()).executeUpdate();
        for (CompetenciasDTO.EstadisticaCommand item : c.getItems()) {
            EquipoJugador jugador = validos.get(item.getJugadorUuid());
            if (jugador == null) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "Un jugador no pertenece a los equipos del partido",
                        "CompetenciasService | jugador invalido: " + item.getJugadorUuid());
            }
            boolean vacio = item.getTantos() == 0 && item.getAsistencias() == 0 && item.getAmonestaciones() == 0
                    && item.getExpulsiones() == 0 && !item.isFigura() && !tieneTexto(item.getObservaciones());
            if (vacio) continue;
            PartidoEstadistica s = new PartidoEstadistica();
            s.setPartido(m);
            s.setJugador(jugador);
            s.setTantos(item.getTantos());
            s.setAsistencias(item.getAsistencias());
            s.setAmonestaciones(item.getAmonestaciones());
            s.setExpulsiones(item.getExpulsiones());
            s.setFigura(item.isFigura());
            s.setObservaciones(limpio(item.getObservaciones()));
            estadisticaRepository.save(s);
        }
        em.flush();
        return estadisticas(partidoUuid);
    }

    // ================================================================ Profesor

    @Transactional(readOnly = true)
    public List<CompetenciasDTO.EquipoDetalle> misEquipos() {
        authService.valid(List.of(PermissionEnum.TEAMS_VIEW_ASSIGNED));
        Company company = authService.getCurrentCompanyOrThrow();
        Profesor profesor = profesorAuthService.getCurrentProfesorOrThrow(company.getId());
        return equiposDelProfesor(company, profesor).stream()
                .sorted(Comparator.comparing(EquipoDeportivo::getNombre, String.CASE_INSENSITIVE_ORDER))
                .map(e -> detalleEquipo(e, false)).toList();
    }

    @Transactional(readOnly = true)
    public CompetenciasDTO.TorneoDetalle torneoDelProfesor(String torneoUuid) {
        authService.valid(List.of(PermissionEnum.TEAMS_VIEW_ASSIGNED));
        Company company = authService.getCurrentCompanyOrThrow();
        Profesor profesor = profesorAuthService.getCurrentProfesorOrThrow(company.getId());
        Torneo torneo = torneo(torneoUuid, company);
        Set<Long> propios = equiposDelProfesor(company, profesor).stream().map(EquipoDeportivo::getId).collect(Collectors.toSet());
        boolean juega = participantes(torneo.getId()).stream().anyMatch(p -> p.getEquipo() != null && propios.contains(p.getEquipo().getId()));
        if (!juega) {
            throw new CustomException(HttpStatus.FORBIDDEN, "Ninguno de tus equipos juega este torneo", "CompetenciasService | torneo ajeno");
        }
        return detalleTorneo(torneo, false);
    }

    // ================================================================ Reglas

    /** Tabla de posiciones: solo partidos JUGADO con resultado, ordenada por puntos, diferencia y a favor. */
    static List<CompetenciasDTO.Posicion> tabla(List<CompetenciasDTO.Participante> participantes,
                                               List<CompetenciasDTO.Partido> partidos, int victoria, int empate, int derrota) {
        Map<String, CompetenciasDTO.Posicion> filas = new LinkedHashMap<>();
        for (CompetenciasDTO.Participante p : participantes) {
            CompetenciasDTO.Posicion fila = new CompetenciasDTO.Posicion();
            fila.setParticipanteUuid(p.getUuid());
            fila.setNombre(p.getNombre());
            fila.setPropio(p.isPropio());
            filas.put(p.getUuid(), fila);
        }
        for (CompetenciasDTO.Partido m : partidos) {
            if (m.getEstado() != PartidoEstadoEnum.JUGADO || m.getTantosLocal() == null || m.getTantosVisitante() == null) continue;
            CompetenciasDTO.Posicion local = filas.get(m.getLocal().getUuid());
            CompetenciasDTO.Posicion visitante = filas.get(m.getVisitante().getUuid());
            if (local == null || visitante == null) continue;
            sumar(local, m.getTantosLocal(), m.getTantosVisitante(), victoria, empate, derrota);
            sumar(visitante, m.getTantosVisitante(), m.getTantosLocal(), victoria, empate, derrota);
        }
        List<CompetenciasDTO.Posicion> tabla = new ArrayList<>(filas.values());
        tabla.sort(Comparator.comparingInt(CompetenciasDTO.Posicion::getPuntos).reversed()
                .thenComparing(Comparator.comparingInt(CompetenciasDTO.Posicion::getDiferencia).reversed())
                .thenComparing(Comparator.comparingInt(CompetenciasDTO.Posicion::getFavor).reversed())
                .thenComparing(CompetenciasDTO.Posicion::getNombre, String.CASE_INSENSITIVE_ORDER));
        return tabla;
    }

    private static void sumar(CompetenciasDTO.Posicion fila, int propios, int ajenos, int victoria, int empate, int derrota) {
        fila.setJugados(fila.getJugados() + 1);
        fila.setFavor(fila.getFavor() + propios);
        fila.setContra(fila.getContra() + ajenos);
        fila.setDiferencia(fila.getFavor() - fila.getContra());
        if (propios > ajenos) {
            fila.setGanados(fila.getGanados() + 1);
            fila.setPuntos(fila.getPuntos() + victoria);
        } else if (propios == ajenos) {
            fila.setEmpatados(fila.getEmpatados() + 1);
            fila.setPuntos(fila.getPuntos() + empate);
        } else {
            fila.setPerdidos(fila.getPerdidos() + 1);
            fila.setPuntos(fila.getPuntos() + derrota);
        }
    }

    // ================================================================ Armado de respuestas

    private List<CompetenciasDTO.EquipoFila> filasEquipos(List<EquipoDeportivo> equipos) {
        Map<Long, Long> jugadores = new HashMap<>();
        if (!equipos.isEmpty()) {
            for (Object[] f : em.createQuery("SELECT j.equipo.id, COUNT(j.id) FROM EquipoJugador j WHERE j.equipo.id IN :e AND j.status = 0"
                            + " AND j.fechaBaja IS NULL GROUP BY j.equipo.id", Object[].class)
                    .setParameter("e", equipos.stream().map(EquipoDeportivo::getId).toList()).getResultList()) {
                jugadores.put((Long) f[0], (Long) f[1]);
            }
        }
        List<CompetenciasDTO.EquipoFila> filas = new ArrayList<>();
        for (EquipoDeportivo e : equipos) {
            CompetenciasDTO.EquipoFila fila = new CompetenciasDTO.EquipoFila();
            fila.setUuid(e.getUuid());
            fila.setNombre(e.getNombre());
            fila.setDisciplina(e.getDisciplina());
            fila.setCategoria(e.getCategoria() != null ? e.getCategoria().getNombre() : null);
            fila.setTemporada(e.getTemporada());
            fila.setEntrenadores(e.getEntrenadores().stream().map(CompetenciasService::nombreProfesor).sorted().toList());
            fila.setJugadores(jugadores.getOrDefault(e.getId(), 0L).intValue());
            fila.setActivo(e.isActivo());
            filas.add(fila);
        }
        return filas;
    }

    private CompetenciasDTO.EquipoDetalle detalleEquipo(EquipoDeportivo e, boolean editable) {
        CompetenciasDTO.EquipoDetalle dto = new CompetenciasDTO.EquipoDetalle();
        dto.setUuid(e.getUuid());
        dto.setNombre(e.getNombre());
        dto.setDisciplina(e.getDisciplina());
        if (e.getCategoria() != null) {
            dto.setCategoriaUuid(e.getCategoria().getUuid());
            dto.setCategoria(e.getCategoria().getNombre());
        }
        if (e.getActividad() != null) {
            dto.setActividadUuid(e.getActividad().getUuid());
            dto.setActividad(e.getActividad().getNombre());
        }
        dto.setTemporada(e.getTemporada());
        dto.setDescripcion(e.getDescripcion());
        dto.setActivo(e.isActivo());
        dto.setEditable(editable);
        e.getEntrenadores().stream()
                .sorted(Comparator.comparing(CompetenciasService::nombreProfesor))
                .forEach(p -> dto.getEntrenadores().add(new CompetenciasDTO.Opcion(p.getUuid(), nombreProfesor(p))));

        List<EquipoJugador> jugadores = em.createQuery("SELECT j FROM EquipoJugador j JOIN FETCH j.member m LEFT JOIN FETCH m.user"
                        + " WHERE j.equipo.id = :e AND j.status = 0", EquipoJugador.class)
                .setParameter("e", e.getId()).getResultList();
        Map<Long, int[]> stats = estadisticasPorJugador(jugadores.stream().map(EquipoJugador::getId).toList());
        for (EquipoJugador j : jugadores) {
            CompetenciasDTO.Jugador dtoJ = jugadorDto(j, stats.get(j.getId()));
            (j.activo() ? dto.getPlantel() : dto.getHistorial()).add(dtoJ);
        }
        dto.getPlantel().sort(Comparator.comparing(CompetenciasDTO.Jugador::getDorsal, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(j -> Objects.toString(j.getApellido(), "")));
        dto.getHistorial().sort(Comparator.comparing(CompetenciasDTO.Jugador::getFechaBaja, Comparator.nullsLast(Comparator.reverseOrder())));

        List<Torneo> torneos = em.createQuery("SELECT DISTINCT p.torneo FROM TorneoParticipante p WHERE p.equipo.id = :e AND p.status = 0"
                        + " AND p.torneo.status = 0", Torneo.class)
                .setParameter("e", e.getId()).getResultList();
        dto.setTorneos(filasTorneos(torneos));
        return dto;
    }

    private CompetenciasDTO.Jugador jugadorDto(EquipoJugador j, int[] s) {
        CompetenciasDTO.Jugador dto = new CompetenciasDTO.Jugador();
        User u = j.getMember().getUser();
        dto.setUuid(j.getUuid());
        dto.setMemberUuid(j.getMember().getUuid());
        dto.setNombre(u != null ? u.getNombre() : null);
        dto.setApellido(u != null ? u.getApellido() : null);
        dto.setDni(u != null ? u.getDni() : null);
        dto.setDorsal(j.getDorsal());
        dto.setPosicion(j.getPosicion());
        dto.setCapitan(j.isCapitan());
        dto.setFechaAlta(j.getFechaAlta());
        dto.setFechaBaja(j.getFechaBaja());
        dto.setMotivoBaja(j.getMotivoBaja());
        if (s != null) {
            dto.setPartidos(s[0]);
            dto.setTantos(s[1]);
            dto.setAsistencias(s[2]);
            dto.setAmonestaciones(s[3]);
            dto.setExpulsiones(s[4]);
            dto.setFiguras(s[5]);
        }
        return dto;
    }

    /** jugadorId -> [partidos, tantos, asistencias, amonestaciones, expulsiones, figuras], solo partidos jugados. */
    private Map<Long, int[]> estadisticasPorJugador(List<Long> jugadorIds) {
        Map<Long, int[]> mapa = new HashMap<>();
        if (jugadorIds.isEmpty()) return mapa;
        for (Object[] f : em.createQuery("SELECT s.jugador.id, COUNT(s.id), SUM(s.tantos), SUM(s.asistencias), SUM(s.amonestaciones), SUM(s.expulsiones),"
                        + " SUM(CASE WHEN s.figura = true THEN 1 ELSE 0 END) FROM PartidoEstadistica s"
                        + " WHERE s.jugador.id IN :j AND s.status = 0 AND s.partido.status = 0"
                        + " AND s.partido.estado = com.blue.open.Code.Entity.Enums.PartidoEstadoEnum.JUGADO GROUP BY s.jugador.id", Object[].class)
                .setParameter("j", jugadorIds).getResultList()) {
            int[] v = new int[6];
            for (int i = 0; i < 6; i++) v[i] = f[i + 1] == null ? 0 : ((Number) f[i + 1]).intValue();
            mapa.put((Long) f[0], v);
        }
        return mapa;
    }

    private List<CompetenciasDTO.TorneoFila> filasTorneos(List<Torneo> torneos) {
        List<CompetenciasDTO.TorneoFila> filas = new ArrayList<>();
        if (torneos.isEmpty()) return filas;
        List<Long> ids = torneos.stream().map(Torneo::getId).toList();
        Map<Long, Long> participantes = contar("SELECT p.torneo.id, COUNT(p.id) FROM TorneoParticipante p WHERE p.torneo.id IN :t AND p.status = 0 GROUP BY p.torneo.id", ids);
        Map<Long, Long> partidos = contar("SELECT m.torneo.id, COUNT(m.id) FROM TorneoPartido m WHERE m.torneo.id IN :t AND m.status = 0 GROUP BY m.torneo.id", ids);
        Map<Long, Long> jugados = contar("SELECT m.torneo.id, COUNT(m.id) FROM TorneoPartido m WHERE m.torneo.id IN :t AND m.status = 0"
                + " AND m.estado = com.blue.open.Code.Entity.Enums.PartidoEstadoEnum.JUGADO GROUP BY m.torneo.id", ids);
        for (Torneo t : torneos) {
            CompetenciasDTO.TorneoFila fila = new CompetenciasDTO.TorneoFila();
            fila.setUuid(t.getUuid());
            fila.setNombre(t.getNombre());
            fila.setDisciplina(t.getDisciplina());
            fila.setCategoria(t.getCategoria() != null ? t.getCategoria().getNombre() : null);
            fila.setTemporada(t.getTemporada());
            fila.setEstado(t.getEstado());
            fila.setFechaInicio(t.getFechaInicio());
            fila.setFechaFin(t.getFechaFin());
            fila.setParticipantes(participantes.getOrDefault(t.getId(), 0L).intValue());
            fila.setPartidos(partidos.getOrDefault(t.getId(), 0L).intValue());
            fila.setPartidosJugados(jugados.getOrDefault(t.getId(), 0L).intValue());
            filas.add(fila);
        }
        return filas;
    }

    private CompetenciasDTO.TorneoDetalle detalleTorneo(Torneo t, boolean editable) {
        CompetenciasDTO.TorneoDetalle dto = new CompetenciasDTO.TorneoDetalle();
        dto.setUuid(t.getUuid());
        dto.setNombre(t.getNombre());
        dto.setDisciplina(t.getDisciplina());
        if (t.getCategoria() != null) {
            dto.setCategoriaUuid(t.getCategoria().getUuid());
            dto.setCategoria(t.getCategoria().getNombre());
        }
        dto.setTemporada(t.getTemporada());
        dto.setFechaInicio(t.getFechaInicio());
        dto.setFechaFin(t.getFechaFin());
        dto.setEstado(t.getEstado());
        dto.setPuntosVictoria(t.getPuntosVictoria());
        dto.setPuntosEmpate(t.getPuntosEmpate());
        dto.setPuntosDerrota(t.getPuntosDerrota());
        dto.setDescripcion(t.getDescripcion());
        dto.setEditable(editable);
        for (TorneoParticipante p : participantes(t.getId())) {
            dto.getParticipantes().add(participanteDto(p));
        }
        List<TorneoPartido> partidos = em.createQuery("SELECT m FROM TorneoPartido m JOIN FETCH m.local l LEFT JOIN FETCH l.equipo JOIN FETCH m.visitante v"
                        + " LEFT JOIN FETCH v.equipo WHERE m.torneo.id = :t AND m.status = 0", TorneoPartido.class)
                .setParameter("t", t.getId()).getResultList();
        Set<Long> conEstadisticas = partidos.isEmpty() ? Set.of() : new LinkedHashSet<>(em.createQuery(
                        "SELECT DISTINCT s.partido.id FROM PartidoEstadistica s WHERE s.partido.id IN :p AND s.status = 0", Long.class)
                .setParameter("p", partidos.stream().map(TorneoPartido::getId).toList()).getResultList());
        partidos.stream()
                .sorted(Comparator.comparing(TorneoPartido::getFechaHora, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(TorneoPartido::getId))
                .forEach(m -> dto.getPartidos().add(partidoDto(m, conEstadisticas)));
        dto.setTabla(tabla(dto.getParticipantes(), dto.getPartidos(), t.getPuntosVictoria(), t.getPuntosEmpate(), t.getPuntosDerrota()));

        for (Object[] f : em.createQuery("SELECT s.jugador.uuid, u.nombre, u.apellido, e.nombre, SUM(s.tantos), SUM(s.asistencias),"
                        + " SUM(CASE WHEN s.figura = true THEN 1 ELSE 0 END) FROM PartidoEstadistica s JOIN s.jugador j JOIN j.member m JOIN m.user u"
                        + " JOIN j.equipo e WHERE s.partido.torneo.id = :t AND s.status = 0 AND s.partido.status = 0"
                        + " AND s.partido.estado = com.blue.open.Code.Entity.Enums.PartidoEstadoEnum.JUGADO"
                        + " GROUP BY s.jugador.uuid, u.nombre, u.apellido, e.nombre ORDER BY SUM(s.tantos) DESC", Object[].class)
                .setParameter("t", t.getId()).setMaxResults(10).getResultList()) {
            CompetenciasDTO.Goleador g = new CompetenciasDTO.Goleador();
            g.setJugadorUuid((String) f[0]);
            g.setNombre(nombre(f[1], f[2]));
            g.setEquipo((String) f[3]);
            g.setTantos(((Number) f[4]).intValue());
            g.setAsistencias(((Number) f[5]).intValue());
            g.setFiguras(((Number) f[6]).intValue());
            if (g.getTantos() > 0 || g.getAsistencias() > 0 || g.getFiguras() > 0) dto.getGoleadores().add(g);
        }
        return dto;
    }

    private CompetenciasDTO.Partido partidoDto(TorneoPartido m, Set<Long> conEstadisticas) {
        CompetenciasDTO.Partido dto = new CompetenciasDTO.Partido();
        dto.setUuid(m.getUuid());
        dto.setRonda(m.getRonda());
        dto.setFechaHora(m.getFechaHora());
        dto.setLugar(m.getLugar());
        dto.setLocal(participanteDto(m.getLocal()));
        dto.setVisitante(participanteDto(m.getVisitante()));
        dto.setTantosLocal(m.getTantosLocal());
        dto.setTantosVisitante(m.getTantosVisitante());
        dto.setEstado(m.getEstado());
        dto.setObservaciones(m.getObservaciones());
        dto.setConEstadisticas(conEstadisticas.contains(m.getId()));
        dto.setConEquipoPropio(m.getLocal().getEquipo() != null || m.getVisitante().getEquipo() != null);
        return dto;
    }

    private static CompetenciasDTO.Participante participanteDto(TorneoParticipante p) {
        return new CompetenciasDTO.Participante(p.getUuid(), p.getNombre(),
                p.getEquipo() != null ? p.getEquipo().getUuid() : null, p.getEquipo() != null);
    }

    // ================================================================ Escritura

    private void aplicar(EquipoDeportivo e, CompetenciasDTO.EquipoCommand c, Company company) {
        e.setNombre(c.getNombre().trim());
        e.setDisciplina(c.getDisciplina().trim());
        e.setCategoria(categoria(c.getCategoriaUuid(), company));
        e.setActividad(actividad(c.getActividadUuid(), company));
        e.setTemporada(limpio(c.getTemporada()));
        e.setDescripcion(limpio(c.getDescripcion()));
        e.setActivo(c.isActivo());
        e.getEntrenadores().clear();
        if (c.getEntrenadorUuids() != null) {
            for (String uuid : new LinkedHashSet<>(c.getEntrenadorUuids())) {
                Profesor p = em.createQuery("SELECT p FROM Profesor p WHERE p.uuid = :u AND p.company.id = :c AND p.status = 0", Profesor.class)
                        .setParameter("u", uuid).setParameter("c", company.getId())
                        .getResultStream().findFirst().orElseThrow(() -> noEncontrado("Profesor", uuid));
                e.getEntrenadores().add(p);
            }
        }
    }

    private void aplicar(Torneo t, CompetenciasDTO.TorneoCommand c, Company company) {
        if (c.getFechaInicio() != null && c.getFechaFin() != null && c.getFechaFin().isBefore(c.getFechaInicio())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "La fecha de fin no puede ser anterior al inicio", "CompetenciasService | fechas invertidas");
        }
        t.setNombre(c.getNombre().trim());
        t.setDisciplina(c.getDisciplina().trim());
        t.setCategoria(categoria(c.getCategoriaUuid(), company));
        t.setTemporada(limpio(c.getTemporada()));
        t.setFechaInicio(c.getFechaInicio());
        t.setFechaFin(c.getFechaFin());
        t.setEstado(c.getEstado());
        t.setPuntosVictoria(c.getPuntosVictoria());
        t.setPuntosEmpate(c.getPuntosEmpate());
        t.setPuntosDerrota(c.getPuntosDerrota());
        t.setDescripcion(limpio(c.getDescripcion()));
    }

    private void aplicar(TorneoPartido m, CompetenciasDTO.PartidoCommand c, Torneo torneo) {
        if (c.getLocalUuid().equals(c.getVisitanteUuid())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "El local y el visitante tienen que ser distintos", "CompetenciasService | mismo participante");
        }
        Map<String, TorneoParticipante> participantes = participantes(torneo.getId()).stream()
                .collect(Collectors.toMap(TorneoParticipante::getUuid, p -> p));
        TorneoParticipante local = participantes.get(c.getLocalUuid());
        TorneoParticipante visitante = participantes.get(c.getVisitanteUuid());
        if (local == null || visitante == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Los equipos tienen que ser participantes del torneo", "CompetenciasService | participante ajeno");
        }
        if (c.getEstado() == PartidoEstadoEnum.JUGADO && (c.getTantosLocal() == null || c.getTantosVisitante() == null)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Para marcarlo como jugado cargá el resultado", "CompetenciasService | jugado sin resultado");
        }
        m.setRonda(c.getRonda().trim());
        m.setFechaHora(c.getFechaHora());
        m.setLugar(limpio(c.getLugar()));
        m.setLocal(local);
        m.setVisitante(visitante);
        boolean conResultado = c.getEstado() == PartidoEstadoEnum.JUGADO;
        m.setTantosLocal(conResultado ? c.getTantosLocal() : null);
        m.setTantosVisitante(conResultado ? c.getTantosVisitante() : null);
        m.setEstado(c.getEstado());
        m.setObservaciones(limpio(c.getObservaciones()));
    }

    private void cerrar(EquipoJugador j, LocalDate fecha, String motivo) {
        LocalDate baja = fecha != null ? fecha : LocalDate.now();
        if (baja.isBefore(j.getFechaAlta())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "La fecha no puede ser anterior al alta en el plantel", "CompetenciasService | baja antes del alta");
        }
        j.setFechaBaja(baja);
        j.setMotivoBaja(motivo);
        j.setCapitan(false);
        jugadorRepository.save(j);
    }

    private void validarDorsal(Long equipoId, Integer dorsal, Long excepto) {
        if (dorsal == null) return;
        boolean ocupado = jugadoresActivos(equipoId).stream()
                .anyMatch(j -> dorsal.equals(j.getDorsal()) && !j.getId().equals(excepto));
        if (ocupado) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "El dorsal " + dorsal + " ya está usado en el plantel", "CompetenciasService | dorsal repetido");
        }
    }

    // ================================================================ Busquedas

    /** Jugadores de los equipos del club del partido que estaban en el plantel en la fecha del partido. */
    private List<EquipoJugador> convocables(TorneoPartido m) {
        List<Long> equipos = new ArrayList<>();
        if (m.getLocal().getEquipo() != null) equipos.add(m.getLocal().getEquipo().getId());
        if (m.getVisitante().getEquipo() != null) equipos.add(m.getVisitante().getEquipo().getId());
        if (equipos.isEmpty()) return List.of();
        LocalDate dia = m.getFechaHora() != null ? m.getFechaHora().toLocalDate() : LocalDate.now();
        return em.createQuery("SELECT j FROM EquipoJugador j JOIN FETCH j.member mb LEFT JOIN FETCH mb.user JOIN FETCH j.equipo"
                        + " WHERE j.equipo.id IN :e AND j.status = 0 AND j.fechaAlta <= :d AND (j.fechaBaja IS NULL OR j.fechaBaja >= :d)", EquipoJugador.class)
                .setParameter("e", equipos).setParameter("d", dia).getResultList().stream()
                .sorted(Comparator.comparing((EquipoJugador j) -> j.getEquipo().getNombre())
                        .thenComparing(EquipoJugador::getDorsal, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private List<EquipoDeportivo> equiposDelProfesor(Company company, Profesor profesor) {
        return em.createQuery("SELECT DISTINCT e FROM EquipoDeportivo e JOIN e.entrenadores p LEFT JOIN FETCH e.categoria"
                        + " WHERE e.company.id = :c AND e.status = 0 AND p.id = :p", EquipoDeportivo.class)
                .setParameter("c", company.getId()).setParameter("p", profesor.getId()).getResultList();
    }

    private List<EquipoJugador> jugadoresActivos(Long equipoId) {
        return em.createQuery("SELECT j FROM EquipoJugador j JOIN FETCH j.member WHERE j.equipo.id = :e AND j.status = 0 AND j.fechaBaja IS NULL", EquipoJugador.class)
                .setParameter("e", equipoId).getResultList();
    }

    private List<TorneoParticipante> participantes(Long torneoId) {
        return em.createQuery("SELECT p FROM TorneoParticipante p LEFT JOIN FETCH p.equipo WHERE p.torneo.id = :t AND p.status = 0 ORDER BY p.nombre", TorneoParticipante.class)
                .setParameter("t", torneoId).getResultList();
    }

    private EquipoDeportivo equipo(String uuid, Company company) {
        return em.createQuery("SELECT e FROM EquipoDeportivo e LEFT JOIN FETCH e.categoria LEFT JOIN FETCH e.actividad WHERE e.uuid = :u AND e.company.id = :c AND e.status = 0",
                        EquipoDeportivo.class)
                .setParameter("u", uuid).setParameter("c", company.getId())
                .getResultStream().findFirst().orElseThrow(() -> noEncontrado("Equipo", uuid));
    }

    private EquipoJugador jugadorActivo(String uuid, Company company) {
        EquipoJugador j = em.createQuery("SELECT j FROM EquipoJugador j JOIN FETCH j.equipo e JOIN FETCH j.member WHERE j.uuid = :u AND e.company.id = :c AND j.status = 0",
                        EquipoJugador.class)
                .setParameter("u", uuid).setParameter("c", company.getId())
                .getResultStream().findFirst().orElseThrow(() -> noEncontrado("Jugador", uuid));
        if (!j.activo()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "El jugador ya no está en el plantel", "CompetenciasService | jugador dado de baja");
        }
        return j;
    }

    private Torneo torneo(String uuid, Company company) {
        return em.createQuery("SELECT t FROM Torneo t LEFT JOIN FETCH t.categoria WHERE t.uuid = :u AND t.company.id = :c AND t.status = 0", Torneo.class)
                .setParameter("u", uuid).setParameter("c", company.getId())
                .getResultStream().findFirst().orElseThrow(() -> noEncontrado("Torneo", uuid));
    }

    private TorneoPartido partido(String uuid, Company company) {
        return em.createQuery("SELECT m FROM TorneoPartido m JOIN FETCH m.torneo t JOIN FETCH m.local l LEFT JOIN FETCH l.equipo JOIN FETCH m.visitante v"
                                + " LEFT JOIN FETCH v.equipo WHERE m.uuid = :u AND t.company.id = :c AND m.status = 0", TorneoPartido.class)
                .setParameter("u", uuid).setParameter("c", company.getId())
                .getResultStream().findFirst().orElseThrow(() -> noEncontrado("Partido", uuid));
    }

    private Categoria categoria(String uuid, Company company) {
        if (!tieneTexto(uuid)) return null;
        return em.createQuery("SELECT c FROM Categoria c WHERE c.uuid = :u AND c.company.id = :c AND c.status = 0", Categoria.class)
                .setParameter("u", uuid).setParameter("c", company.getId())
                .getResultStream().findFirst().orElseThrow(() -> noEncontrado("Categoría", uuid));
    }

    private Actividad actividad(String uuid, Company company) {
        if (!tieneTexto(uuid)) return null;
        return em.createQuery("SELECT a FROM Actividad a WHERE a.uuid = :u AND a.company.id = :c AND a.status = 0", Actividad.class)
                .setParameter("u", uuid).setParameter("c", company.getId())
                .getResultStream().findFirst().orElseThrow(() -> noEncontrado("Actividad", uuid));
    }

    private Map<Long, Long> contar(String jpql, List<Long> ids) {
        Map<Long, Long> mapa = new HashMap<>();
        for (Object[] f : em.createQuery(jpql, Object[].class).setParameter("t", ids).getResultList()) {
            mapa.put((Long) f[0], (Long) f[1]);
        }
        return mapa;
    }

    private Company admin() {
        authService.valid(List.of(PermissionEnum.TEAMS_MANAGE));
        return authService.getCurrentCompanyOrThrow();
    }

    // ================================================================ Utilidades

    private static void bajaLogica(com.blue.open.Core.Entity.Db.AbstractEntity entidad) {
        entidad.setStatus(-1);
        entidad.setDateDeleted(new Timestamp(System.currentTimeMillis()));
    }

    private static CustomException noEncontrado(String que, String uuid) {
        return new CustomException(HttpStatus.NOT_FOUND, que + " no encontrado", "CompetenciasService | " + que + ": " + uuid);
    }

    private static String nombreProfesor(Profesor p) {
        User u = p.getUser();
        return u == null ? "" : nombre(u.getNombre(), u.getApellido());
    }

    private static String nombreSocio(Member m) {
        User u = m.getUser();
        return u == null ? m.getUuid() : nombre(u.getNombre(), u.getApellido());
    }

    private static String nombre(Object nombre, Object apellido) {
        return (Objects.toString(nombre, "") + " " + Objects.toString(apellido, "")).trim();
    }

    private static String like(String texto) {
        return "%" + texto.trim().toLowerCase(Locale.ROOT) + "%";
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static String limpio(String valor) {
        return tieneTexto(valor) ? valor.trim() : null;
    }
}
