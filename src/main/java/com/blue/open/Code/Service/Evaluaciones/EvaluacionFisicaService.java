package com.blue.open.Code.Service.Evaluaciones;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.EvaluacionFisicaDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Configuration;
import com.blue.open.Code.Entity.Db.EvaluacionFisica;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.Profesor;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Repository.ActividadProfesorRepository;
import com.blue.open.Code.Repository.ActividadRepository;
import com.blue.open.Code.Repository.ConfigurationRepository;
import com.blue.open.Code.Repository.EvaluacionFisicaRepository;
import com.blue.open.Code.Service.MemberAuthService;
import com.blue.open.Code.Service.ProfesorAuthService;
import com.blue.open.Core.Auth.BaseRoleEnum;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Evaluaciones fisicas: modulo opcional por club (configurations: {@value #CONFIG}).
 *
 * Alcance, siempre dentro del club del header:
 * - ADMIN: todos los socios; activa o desactiva el modulo y decide si los profesores registran.
 * - PROFESOR: solo si el club lo habilito, y solo alumnos con inscripcion activa en sus actividades.
 *   Edita o borra solo lo que cargo el.
 * - Socio (portal): lo propio y lo de su grupo familiar, si el modulo esta activo.
 * Recepcion no tiene acceso. Son datos de seguimiento deportivo: no se generan diagnosticos.
 */
@Service
@Slf4j
public class EvaluacionFisicaService {

    static final String CONFIG = "evaluaciones.config";
    private static final int MAX_SOCIOS = 300;

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
    private ConfigurationRepository configurationRepository;

    @Autowired
    private EvaluacionFisicaRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private record Alcance(Company company, User user, Profesor profesor, Set<Long> actividades) {
        boolean esAdmin() {
            return profesor == null;
        }
    }

    // ---------------------------------------------------------------- Configuracion y acceso

    @Transactional(readOnly = true)
    public EvaluacionFisicaDTO.Acceso acceso() {
        authService.valid(List.of(PermissionEnum.PHYSICAL_EVALUATION_MANAGE));
        Company company = authService.getCurrentCompanyOrThrow();
        EvaluacionFisicaDTO.Config config = config(company);
        boolean admin = esAdmin();
        EvaluacionFisicaDTO.Acceso acceso = new EvaluacionFisicaDTO.Acceso();
        acceso.setEsAdmin(admin);
        acceso.setHabilitado(config.isHabilitado());
        acceso.setPuedeRegistrar(config.isHabilitado() && (admin || config.isProfesoresPuedenRegistrar()));
        acceso.setConfig(admin ? config : null);
        return acceso;
    }

    @Transactional
    public EvaluacionFisicaDTO.Acceso guardarConfig(EvaluacionFisicaDTO.Config request) {
        authService.valid(List.of(PermissionEnum.PHYSICAL_EVALUATION_MANAGE));
        if (!esAdmin()) {
            throw new CustomException(HttpStatus.FORBIDDEN, "Solo un administrador puede configurar el módulo",
                    "EvaluacionFisicaService | config sin ADMIN");
        }
        Company company = authService.getCurrentCompanyOrThrow();
        Configuration configuracion = configurationRepository.findFirstByNameAndCompany(CONFIG, company);
        if (configuracion == null) {
            configuracion = new Configuration();
            configuracion.setName(CONFIG);
            configuracion.setCompany(company);
            configuracion.setGlobal(false);
        }
        try {
            configuracion.setValue(objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            throw new CustomException("No se pudo guardar la configuración");
        }
        configurationRepository.save(configuracion);
        return acceso();
    }

    // ---------------------------------------------------------------- Socios y evaluaciones

    /** Socios que el usuario puede evaluar, con su ultima evaluacion y la proxima prevista. */
    @Transactional(readOnly = true)
    public List<EvaluacionFisicaDTO.Fila> socios(String search) {
        Alcance alcance = alcance();
        Map<String, Object> params = new HashMap<>();
        params.put("c", alcance.company().getId());
        StringBuilder jpql;
        if (alcance.esAdmin()) {
            jpql = new StringBuilder("SELECT m.id, m.uuid, u.nombre, u.apellido, u.dni, '' FROM Member m JOIN m.user u"
                    + " WHERE m.company.id = :c AND m.status = 0");
        } else {
            if (alcance.actividades().isEmpty()) {
                return List.of();
            }
            jpql = new StringBuilder("SELECT m.id, m.uuid, u.nombre, u.apellido, u.dni, a.nombre FROM InscripcionActividad i"
                    + " JOIN i.member m JOIN m.user u JOIN i.actividad a"
                    + " WHERE i.company.id = :c AND i.status = 0 AND m.status = 0 AND a.id IN :acts");
            params.put("acts", alcance.actividades());
        }
        if (tieneTexto(search)) {
            jpql.append(" AND (LOWER(CONCAT(u.nombre, ' ', u.apellido)) LIKE :s OR u.dni LIKE :s)");
            params.put("s", "%" + search.trim().toLowerCase(Locale.ROOT) + "%");
        }
        jpql.append(" ORDER BY u.apellido, u.nombre");
        TypedQuery<Object[]> query = em.createQuery(jpql.toString(), Object[].class);
        params.forEach(query::setParameter);

        Map<Long, EvaluacionFisicaDTO.Fila> porMember = new LinkedHashMap<>();
        for (Object[] f : query.getResultList()) {
            EvaluacionFisicaDTO.Fila fila = porMember.computeIfAbsent((Long) f[0], k -> {
                EvaluacionFisicaDTO.Fila nueva = new EvaluacionFisicaDTO.Fila();
                nueva.setMemberUuid((String) f[1]);
                nueva.setNombre((String) f[2]);
                nueva.setApellido((String) f[3]);
                nueva.setDni((String) f[4]);
                return nueva;
            });
            String actividad = (String) f[5];
            if (tieneTexto(actividad)) {
                fila.setActividades(fila.getActividades() == null ? actividad : fila.getActividades() + ", " + actividad);
            }
            if (porMember.size() >= MAX_SOCIOS) {
                break;
            }
        }
        if (porMember.isEmpty()) {
            return List.of();
        }
        LocalDate hoy = LocalDate.now();
        for (Object[] f : em.createQuery("SELECT e.member.id, e.fecha, e.proximaEvaluacion FROM EvaluacionFisica e"
                        + " WHERE e.company.id = :c AND e.status = 0 AND e.member.id IN :m ORDER BY e.fecha", Object[].class)
                .setParameter("c", alcance.company().getId())
                .setParameter("m", porMember.keySet())
                .getResultList()) {
            EvaluacionFisicaDTO.Fila fila = porMember.get((Long) f[0]);
            fila.setCantidad(fila.getCantidad() + 1);
            fila.setUltimaEvaluacion((LocalDate) f[1]);
            fila.setProximaEvaluacion((LocalDate) f[2]);
            fila.setVencida(f[2] != null && ((LocalDate) f[2]).isBefore(hoy));
        }
        return new ArrayList<>(porMember.values());
    }

    @Transactional(readOnly = true)
    public EvaluacionFisicaDTO.Historial historial(String memberUuid) {
        Alcance alcance = alcance();
        Member member = socioAutorizado(memberUuid, alcance);
        return historial(member, alcance.company(), alcance, true);
    }

    @Transactional
    public EvaluacionFisicaDTO.Historial crear(String memberUuid, EvaluacionFisicaDTO.Command command) {
        Alcance alcance = alcanceParaRegistrar();
        Member member = socioAutorizado(memberUuid, alcance);
        EvaluacionFisica evaluacion = new EvaluacionFisica();
        evaluacion.setCompany(alcance.company());
        evaluacion.setMember(member);
        evaluacion.setRegistradaPor(alcance.user());
        aplicar(evaluacion, command);
        repository.save(evaluacion);
        return historial(member, alcance.company(), alcance, true);
    }

    @Transactional
    public EvaluacionFisicaDTO.Historial editar(String uuid, EvaluacionFisicaDTO.Command command) {
        Alcance alcance = alcanceParaRegistrar();
        EvaluacionFisica evaluacion = evaluacionEditable(uuid, alcance);
        aplicar(evaluacion, command);
        repository.save(evaluacion);
        return historial(evaluacion.getMember(), alcance.company(), alcance, true);
    }

    @Transactional
    public EvaluacionFisicaDTO.Historial borrar(String uuid) {
        Alcance alcance = alcanceParaRegistrar();
        EvaluacionFisica evaluacion = evaluacionEditable(uuid, alcance);
        evaluacion.setStatus(-1);
        evaluacion.setDateDeleted(new Timestamp(System.currentTimeMillis()));
        repository.save(evaluacion);
        return historial(evaluacion.getMember(), alcance.company(), alcance, true);
    }

    // ---------------------------------------------------------------- Portal del socio

    @Transactional(readOnly = true)
    public EvaluacionFisicaDTO.Historial delSocio(String memberUuid) {
        authService.valid(List.of(PermissionEnum.PORTAL_PHYSICAL_EVALUATION_VIEW));
        Company company = authService.getCurrentCompanyOrThrow();
        Member member = tieneTexto(memberUuid)
                ? memberAuthService.resolveMemberOfFamilyOrThrow(memberUuid, company.getId())
                : memberAuthService.getCurrentMemberOrThrow(company.getId());
        if (!config(company).isHabilitado()) {
            EvaluacionFisicaDTO.Historial vacio = new EvaluacionFisicaDTO.Historial();
            vacio.setMemberUuid(member.getUuid());
            return vacio;
        }
        return historial(member, company, null, false);
    }

    // ---------------------------------------------------------------- Reglas

    /** IMC = peso (kg) / altura (m)^2, con dos decimales. Null si falta alguno. */
    static BigDecimal imc(BigDecimal pesoKg, BigDecimal alturaCm) {
        if (pesoKg == null || alturaCm == null || alturaCm.signum() <= 0) {
            return null;
        }
        BigDecimal metros = alturaCm.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return pesoKg.divide(metros.multiply(metros), 2, RoundingMode.HALF_UP);
    }

    private void aplicar(EvaluacionFisica e, EvaluacionFisicaDTO.Command c) {
        if (c.getFecha().isAfter(LocalDate.now())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "La evaluación no puede tener fecha futura",
                    "EvaluacionFisicaService | fecha futura");
        }
        if (c.getProximaEvaluacion() != null && !c.getProximaEvaluacion().isAfter(c.getFecha())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "La próxima evaluación tiene que ser posterior a esta",
                    "EvaluacionFisicaService | proxima invalida");
        }
        boolean algunDato = java.util.stream.Stream.of(c.getPesoKg(), c.getAlturaCm(), c.getGrasaCorporalPct(), c.getMasaMuscularKg(),
                c.getCinturaCm(), c.getCaderaCm(), c.getPechoCm(), c.getBrazoCm(), c.getMusloCm(), c.getPantorrillaCm())
                .anyMatch(Objects::nonNull) || tieneTexto(c.getObservaciones());
        if (!algunDato) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Cargá al menos una medición u observación",
                    "EvaluacionFisicaService | evaluacion vacia");
        }
        e.setFecha(c.getFecha());
        e.setProfesional(limpio(c.getProfesional()));
        e.setPesoKg(c.getPesoKg());
        e.setAlturaCm(c.getAlturaCm());
        e.setImc(imc(c.getPesoKg(), c.getAlturaCm()));
        e.setGrasaCorporalPct(c.getGrasaCorporalPct());
        e.setMasaMuscularKg(c.getMasaMuscularKg());
        e.setCinturaCm(c.getCinturaCm());
        e.setCaderaCm(c.getCaderaCm());
        e.setPechoCm(c.getPechoCm());
        e.setBrazoCm(c.getBrazoCm());
        e.setMusloCm(c.getMusloCm());
        e.setPantorrillaCm(c.getPantorrillaCm());
        e.setObservaciones(limpio(c.getObservaciones()));
        e.setRecomendaciones(limpio(c.getRecomendaciones()));
        e.setProximaEvaluacion(c.getProximaEvaluacion());
    }

    private EvaluacionFisicaDTO.Historial historial(Member member, Company company, Alcance alcance, boolean conEdicion) {
        EvaluacionFisicaDTO.Historial dto = new EvaluacionFisicaDTO.Historial();
        User u = member.getUser();
        dto.setMemberUuid(member.getUuid());
        dto.setNombre(u != null ? u.getNombre() : null);
        dto.setApellido(u != null ? u.getApellido() : null);
        dto.setDni(u != null ? u.getDni() : null);
        boolean puedeRegistrar = conEdicion && alcance != null && puedeRegistrar(alcance);
        dto.setPuedeRegistrar(puedeRegistrar);
        List<EvaluacionFisica> evaluaciones = em.createQuery(
                        "SELECT e FROM EvaluacionFisica e LEFT JOIN FETCH e.registradaPor WHERE e.company.id = :c AND e.member.id = :m"
                                + " AND e.status = 0 ORDER BY e.fecha DESC, e.id DESC", EvaluacionFisica.class)
                .setParameter("c", company.getId())
                .setParameter("m", member.getId())
                .getResultList();
        for (EvaluacionFisica e : evaluaciones) {
            EvaluacionFisicaDTO.Respuesta r = new EvaluacionFisicaDTO.Respuesta();
            r.setUuid(e.getUuid());
            r.setFecha(e.getFecha());
            r.setProfesional(e.getProfesional());
            User registro = e.getRegistradaPor();
            r.setRegistradaPor(registro != null ? (Objects.toString(registro.getNombre(), "") + " " + Objects.toString(registro.getApellido(), "")).trim() : null);
            r.setPesoKg(e.getPesoKg());
            r.setAlturaCm(e.getAlturaCm());
            r.setImc(e.getImc());
            r.setGrasaCorporalPct(e.getGrasaCorporalPct());
            r.setMasaMuscularKg(e.getMasaMuscularKg());
            r.setCinturaCm(e.getCinturaCm());
            r.setCaderaCm(e.getCaderaCm());
            r.setPechoCm(e.getPechoCm());
            r.setBrazoCm(e.getBrazoCm());
            r.setMusloCm(e.getMusloCm());
            r.setPantorrillaCm(e.getPantorrillaCm());
            r.setObservaciones(e.getObservaciones());
            r.setRecomendaciones(e.getRecomendaciones());
            r.setProximaEvaluacion(e.getProximaEvaluacion());
            r.setEditable(puedeRegistrar && (alcance.esAdmin()
                    || (registro != null && registro.getId().equals(alcance.user().getId()))));
            dto.getEvaluaciones().add(r);
        }
        return dto;
    }

    private EvaluacionFisica evaluacionEditable(String uuid, Alcance alcance) {
        EvaluacionFisica evaluacion = em.createQuery("SELECT e FROM EvaluacionFisica e JOIN FETCH e.member m LEFT JOIN FETCH e.registradaPor"
                                + " WHERE e.uuid = :u AND e.company.id = :c AND e.status = 0", EvaluacionFisica.class)
                .setParameter("u", uuid).setParameter("c", alcance.company().getId())
                .getResultStream().findFirst()
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Evaluación no encontrada",
                        "EvaluacionFisicaService | uuid: " + uuid));
        socioAutorizado(evaluacion.getMember().getUuid(), alcance);
        if (!alcance.esAdmin() && (evaluacion.getRegistradaPor() == null
                || !evaluacion.getRegistradaPor().getId().equals(alcance.user().getId()))) {
            throw new CustomException(HttpStatus.FORBIDDEN, "Solo podés modificar las evaluaciones que cargaste vos",
                    "EvaluacionFisicaService | evaluacion ajena: " + uuid);
        }
        return evaluacion;
    }

    private Member socioAutorizado(String memberUuid, Alcance alcance) {
        Member member = memberAuthService.resolveMemberByUuid(memberUuid, alcance.company().getId());
        if (alcance.esAdmin()) {
            return member;
        }
        Long cantidad = alcance.actividades().isEmpty() ? 0L : em.createQuery(
                        "SELECT COUNT(i.id) FROM InscripcionActividad i WHERE i.member.id = :m AND i.actividad.id IN :a AND i.status = 0", Long.class)
                .setParameter("m", member.getId()).setParameter("a", alcance.actividades()).getSingleResult();
        if (cantidad == 0) {
            throw new CustomException(HttpStatus.FORBIDDEN, "Este socio no es alumno de tus actividades",
                    "EvaluacionFisicaService | member ajeno: " + memberUuid);
        }
        return member;
    }

    private Alcance alcance() {
        authService.valid(List.of(PermissionEnum.PHYSICAL_EVALUATION_MANAGE));
        Company company = authService.getCurrentCompanyOrThrow();
        User user = authService.getActualOrThrow();
        EvaluacionFisicaDTO.Config config = config(company);
        if (!config.isHabilitado()) {
            throw new CustomException(HttpStatus.FORBIDDEN, "El módulo de evaluaciones físicas no está activo en el gimnasio",
                    "EvaluacionFisicaService | modulo apagado");
        }
        if (esAdmin()) {
            return new Alcance(company, user, null, Set.of());
        }
        if (!config.isProfesoresPuedenRegistrar()) {
            throw new CustomException(HttpStatus.FORBIDDEN, "El gimnasio no habilitó a los profesores para las evaluaciones físicas",
                    "EvaluacionFisicaService | profesores no habilitados");
        }
        Profesor profesor = profesorAuthService.requirePortalEligible(profesorAuthService.getCurrentProfesorOrThrow(company.getId()));
        Set<Long> actividades = new HashSet<>(actividadProfesorRepository.findActividadIdsByProfesorId(profesor.getId()));
        actividades.retainAll(new HashSet<>(actividadRepository.findIdsActivasByCompanyId(company.getId())));
        return new Alcance(company, user, profesor, actividades);
    }

    private Alcance alcanceParaRegistrar() {
        Alcance alcance = alcance();
        if (!puedeRegistrar(alcance)) {
            throw new CustomException(HttpStatus.FORBIDDEN, "No tenés permiso para registrar evaluaciones",
                    "EvaluacionFisicaService | sin permiso de registro");
        }
        return alcance;
    }

    private boolean puedeRegistrar(Alcance alcance) {
        // * alcance() ya exige modulo activo y, para profesores, que el club los habilite
        return alcance != null;
    }

    private boolean esAdmin() {
        return authService.validBoolean(List.of(BaseRoleEnum.ADMIN, BaseRoleEnum.SYSADMIN));
    }

    private EvaluacionFisicaDTO.Config config(Company company) {
        Configuration configuracion = configurationRepository.findFirstByNameAndCompany(CONFIG, company);
        if (configuracion == null || !tieneTexto(configuracion.getValue())) {
            return new EvaluacionFisicaDTO.Config();
        }
        try {
            return objectMapper.readValue(configuracion.getValue(), EvaluacionFisicaDTO.Config.class);
        } catch (JsonProcessingException e) {
            log.warn("EvaluacionFisicaService | config invalida | companyId={}", company.getId(), e);
            return new EvaluacionFisicaDTO.Config();
        }
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static String limpio(String valor) {
        return tieneTexto(valor) ? valor.trim() : null;
    }
}
