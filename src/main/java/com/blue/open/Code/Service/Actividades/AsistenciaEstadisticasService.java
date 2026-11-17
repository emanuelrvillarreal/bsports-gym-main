package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.AsistenciaAlumnoDetalleDTO;
import com.blue.open.Code.Entity.DTO.AsistenciaAlumnoFilaDTO;
import com.blue.open.Code.Entity.DTO.AsistenciaEstadisticasDTO;
import com.blue.open.Code.Entity.DTO.AsistenciaEstadisticasRequestDTO;
import com.blue.open.Code.Entity.DTO.AsistenciaMesDTO;
import com.blue.open.Code.Entity.DTO.AsistenciaUmbralesDTO;
import com.blue.open.Code.Entity.DTO.TrayectoriaDeportivaDTO;
import com.blue.open.Code.Entity.Db.ActividadHorario;
import com.blue.open.Code.Entity.Db.ActividadProfesor;
import com.blue.open.Code.Entity.Db.Categoria;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.InscripcionActividad;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.Profesor;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.AsistenciaActividadEstadoEnum;
import com.blue.open.Code.Repository.ActividadProfesorRepository;
import com.blue.open.Code.Repository.ActividadRepository;
import com.blue.open.Code.Repository.AsistenciaActividadRepository;
import com.blue.open.Code.Repository.InscripcionActividadRepository;
import com.blue.open.Code.Repository.ProfesorRepository;
import com.blue.open.Code.Repository.SlotRepository;
import com.blue.open.Code.Service.MemberAuthService;
import com.blue.open.Code.Service.ProfesorAuthService;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Seguimiento de asistencia y trayectoria deportiva, calculado sobre las clases y asistencias reales.
 *
 * Reglas del calculo (por periodo de inscripcion):
 * - Clase realizada: slot de la actividad no cancelado, ya dictado y con la planilla cargada (al menos un
 *   registro). Las clases sin planilla, las canceladas, las futuras y las anteriores a la inscripcion
 *   (o posteriores a la baja) no cuentan.
 * - En una clase realizada, el alumno sin registro cuenta como ausente; si la clase es de otro horario del
 *   que eligio y no tiene registro, no cuenta.
 * - Porcentaje = presentes / clases realizadas x 100. Las ausencias justificadas se informan aparte.
 *
 * Alcance: el ADMIN ve todo el club, el profesor solo sus actividades asignadas y el socio solo lo suyo y
 * lo de su grupo familiar. Todas las consultas filtran por el club del header X-Company-Uuid.
 * Carga todo con tres consultas por pedido (inscripciones, clases y marcas), sin N+1.
 */
@Service
public class AsistenciaEstadisticasService {

    private static final LocalDate INICIO_HISTORIA = LocalDate.of(2000, 1, 1);
    private static final int MAX_ALERTAS = 50;

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
    private InscripcionActividadRepository inscripcionActividadRepository;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private AsistenciaActividadRepository asistenciaActividadRepository;

    @Autowired
    private AsistenciaUmbralesService umbralesService;

    // ---------------------------------------------------------------- Backoffice (ADMIN)

    @Transactional(readOnly = true)
    public AsistenciaEstadisticasDTO estadisticasInstitucionales(AsistenciaEstadisticasRequestDTO request) {
        authService.valid(List.of(PermissionEnum.ACTIVITY_ATTENDANCE_STATS));
        Company company = authService.getCurrentCompanyOrThrow();
        Set<Long> actividadIds = alcanceInstitucional(request, company.getId());
        return calcular(company, actividadIds, request, null, true);
    }

    @Transactional(readOnly = true)
    public AsistenciaAlumnoDetalleDTO detalleInstitucional(String inscripcionUuid, LocalDate desde, LocalDate hasta) {
        authService.valid(List.of(PermissionEnum.ACTIVITY_ATTENDANCE_STATS));
        Company company = authService.getCurrentCompanyOrThrow();
        return detalle(company, resolverInscripcion(inscripcionUuid, company.getId()), desde, hasta);
    }

    @Transactional(readOnly = true)
    public TrayectoriaDeportivaDTO trayectoriaInstitucional(String memberUuid) {
        authService.valid(List.of(PermissionEnum.ACTIVITY_ATTENDANCE_STATS));
        Company company = authService.getCurrentCompanyOrThrow();
        Member member = memberAuthService.resolveMemberByUuid(memberUuid, company.getId());
        return trayectoria(company, member, null);
    }

    // ---------------------------------------------------------------- Portal del profesor

    @Transactional(readOnly = true)
    public AsistenciaEstadisticasDTO estadisticasProfesor(AsistenciaEstadisticasRequestDTO request) {
        authService.valid(List.of(PermissionEnum.ACTIVITY_ATTENDANCE_MANAGE));
        Company company = authService.getCurrentCompanyOrThrow();
        Set<Long> propias = actividadesDelProfesor(company.getId());

        Set<Long> actividadIds = new LinkedHashSet<>(propias);
        if (tieneTexto(request.getActividadUuid())) {
            Long actividadId = resolverActividadId(request.getActividadUuid(), company.getId());
            if (!propias.contains(actividadId)) {
                throw sinAcceso("actividadUuid: " + request.getActividadUuid());
            }
            actividadIds = Set.of(actividadId);
        }
        // * El profesor no filtra por otros profesores: siempre son sus actividades
        request.setProfesorUuid(null);
        return calcular(company, actividadIds, request, null, false);
    }

    @Transactional(readOnly = true)
    public AsistenciaAlumnoDetalleDTO detalleProfesor(String inscripcionUuid, LocalDate desde, LocalDate hasta) {
        authService.valid(List.of(PermissionEnum.ACTIVITY_ATTENDANCE_MANAGE));
        Company company = authService.getCurrentCompanyOrThrow();
        InscripcionActividad inscripcion = resolverInscripcion(inscripcionUuid, company.getId());
        if (!actividadesDelProfesor(company.getId()).contains(inscripcion.getActividad().getId())) {
            throw sinAcceso("inscripcionUuid: " + inscripcionUuid);
        }
        return detalle(company, inscripcion, desde, hasta);
    }

    /** Trayectoria del alumno limitada a las actividades del profesor. */
    @Transactional(readOnly = true)
    public TrayectoriaDeportivaDTO trayectoriaProfesor(String memberUuid) {
        authService.valid(List.of(PermissionEnum.ACTIVITY_ATTENDANCE_MANAGE));
        Company company = authService.getCurrentCompanyOrThrow();
        Set<Long> propias = actividadesDelProfesor(company.getId());
        Member member = memberAuthService.resolveMemberByUuid(memberUuid, company.getId());
        TrayectoriaDeportivaDTO dto = trayectoria(company, member, propias);
        if (dto.getActividades().isEmpty()) {
            throw sinAcceso("memberUuid: " + memberUuid);
        }
        return dto;
    }

    // ---------------------------------------------------------------- Portal del socio

    /** Asistencias del socio actual o de un familiar de su grupo, en todas sus actividades. */
    @Transactional(readOnly = true)
    public AsistenciaEstadisticasDTO asistenciasSocio(String memberUuid, LocalDate desde, LocalDate hasta) {
        authService.valid(List.of(PermissionEnum.PORTAL_ACTIVITIES_VIEW));
        Company company = authService.getCurrentCompanyOrThrow();
        Member member = resolverSocio(memberUuid, company.getId());

        Set<Long> actividadIds = inscripcionActividadRepository.findHistorialByMemberId(company.getId(), member.getId())
                .stream().map(i -> i.getActividad().getId()).collect(Collectors.toCollection(LinkedHashSet::new));

        AsistenciaEstadisticasRequestDTO request = new AsistenciaEstadisticasRequestDTO();
        request.setDesde(desde);
        request.setHasta(hasta);
        request.setMemberUuid(member.getUuid());
        request.setEstadoInscripcion("TODAS");
        request.setSize(100);
        return calcular(company, actividadIds, request, member.getId(), false);
    }

    @Transactional(readOnly = true)
    public AsistenciaAlumnoDetalleDTO detalleSocio(String inscripcionUuid, LocalDate desde, LocalDate hasta) {
        authService.valid(List.of(PermissionEnum.PORTAL_ACTIVITIES_VIEW));
        Company company = authService.getCurrentCompanyOrThrow();
        InscripcionActividad inscripcion = resolverInscripcion(inscripcionUuid, company.getId());
        // ! Valida que la inscripcion sea del socio o de alguien de su grupo familiar
        resolverSocio(inscripcion.getMember().getUuid(), company.getId());
        return detalle(company, inscripcion, desde, hasta);
    }

    @Transactional(readOnly = true)
    public TrayectoriaDeportivaDTO trayectoriaSocio(String memberUuid) {
        authService.valid(List.of(PermissionEnum.PORTAL_ACTIVITIES_VIEW));
        Company company = authService.getCurrentCompanyOrThrow();
        return trayectoria(company, resolverSocio(memberUuid, company.getId()), null);
    }

    // ---------------------------------------------------------------- Calculo

    private AsistenciaEstadisticasDTO calcular(
            Company company,
            Set<Long> actividadIds,
            AsistenciaEstadisticasRequestDTO request,
            Long memberIdFiltro,
            boolean conRankingProfesores) {
        AsistenciaUmbralesDTO umbrales = umbralesService.obtener(company);
        LocalDate hoy = LocalDate.now();
        Rango rango = rango(request.getDesde(), request.getHasta(), hoy);

        AsistenciaEstadisticasDTO dto = new AsistenciaEstadisticasDTO();
        dto.setUmbrales(umbrales);
        PageRequest pageable = PageRequest.of(request.getPage() - 1, request.getSize());
        if (actividadIds.isEmpty()) {
            dto.setAlumnos(new PageImpl<>(List.of(), pageable, 0));
            return dto;
        }

        boolean incluirBajas = "TODAS".equals(request.getEstadoInscripcion());
        List<InscripcionActividad> inscripciones = inscripcionActividadRepository.findPeriodosByActividadIdIn(
                company.getId(), actividadIds, rango.desde(), rango.hasta(), incluirBajas);
        inscripciones = filtrar(inscripciones, request);

        Set<Long> memberIds = memberIdFiltro != null ? Set.of(memberIdFiltro) : null;
        Datos datos = cargarDatos(company.getId(), actividadIds, memberIds, rango);
        Map<Long, List<Profesor>> profesores = profesoresPorActividad(actividadIds);

        List<Calculo> calculos = new ArrayList<>();
        for (InscripcionActividad inscripcion : inscripciones) {
            calculos.add(computar(inscripcion, datos, rango, umbrales, hoy, false, profesores));
        }

        dto.setResumen(resumen(calculos, datos, actividadIds, rango));
        dto.setActividades(rankingActividades(calculos, datos, rango, profesores));
        if (conRankingProfesores) {
            dto.setProfesores(rankingProfesores(calculos, profesores));
        }
        dto.setEvolucionMensual(evolucion(calculos));

        List<AsistenciaAlumnoFilaDTO> filas = calculos.stream().map(c -> c.fila).toList();
        dto.setAlertas(filas.stream()
                .filter(f -> !f.getAlertas().isEmpty())
                .sorted(Comparator.comparingInt((AsistenciaAlumnoFilaDTO f) -> f.getAlertas().size()).reversed()
                        .thenComparing(f -> f.getPorcentaje() == null ? 0d : f.getPorcentaje()))
                .limit(MAX_ALERTAS)
                .toList());

        List<AsistenciaAlumnoFilaDTO> visibles = filas.stream()
                .filter(f -> request.getNivel() == null || request.getNivel().equals(f.getNivel()))
                .filter(f -> !request.isSoloAlertas() || !f.getAlertas().isEmpty())
                .sorted(orden(request.getOrden()))
                .toList();
        int desdeIndice = Math.min((int) pageable.getOffset(), visibles.size());
        int hastaIndice = Math.min(desdeIndice + pageable.getPageSize(), visibles.size());
        dto.setAlumnos(new PageImpl<>(visibles.subList(desdeIndice, hastaIndice), pageable, visibles.size()));
        return dto;
    }

    private AsistenciaAlumnoDetalleDTO detalle(
            Company company, InscripcionActividad inscripcion, LocalDate desde, LocalDate hasta) {
        AsistenciaUmbralesDTO umbrales = umbralesService.obtener(company);
        LocalDate hoy = LocalDate.now();
        Rango rango = rango(desde, hasta, hoy);
        Set<Long> actividadIds = Set.of(inscripcion.getActividad().getId());
        Datos datos = cargarDatos(company.getId(), actividadIds, Set.of(inscripcion.getMember().getId()), rango);
        Calculo calculo = computar(inscripcion, datos, rango, umbrales, hoy, true,
                profesoresPorActividad(actividadIds));

        AsistenciaAlumnoDetalleDTO dto = new AsistenciaAlumnoDetalleDTO();
        dto.setAlumno(calculo.fila);
        dto.setEvolucionMensual(evolucion(List.of(calculo)));
        List<AsistenciaAlumnoDetalleDTO.Clase> clases = new ArrayList<>(calculo.clases_);
        java.util.Collections.reverse(clases);
        dto.setClases(clases);
        return dto;
    }

    private Calculo computar(
            InscripcionActividad inscripcion,
            Datos datos,
            Rango rango,
            AsistenciaUmbralesDTO umbrales,
            LocalDate hoy,
            boolean conClases,
            Map<Long, List<Profesor>> profesores) {
        Calculo c = new Calculo();
        c.inscripcion = inscripcion;

        LocalDate inicio = max(rango.desde(), inscripcion.getFechaInscripcion());
        LocalDate fin = inscripcion.getFechaBaja() != null ? min(rango.hasta(), inscripcion.getFechaBaja()) : rango.hasta();
        Set<Long> horarioIds = inscripcion.getHorarios().stream()
                .map(ActividadHorario::getId).collect(Collectors.toSet());
        Map<Long, AsistenciaActividadEstadoEnum> marcas =
                datos.marcas.getOrDefault(inscripcion.getMember().getId(), Map.of());

        int consecutivas = 0;
        int clasesDesdeUltimoPresente = 0;
        for (ClaseRealizada clase : datos.clases.getOrDefault(inscripcion.getActividad().getId(), List.of())) {
            LocalDate fecha = clase.inicio().toLocalDate();
            if (fecha.isBefore(inicio) || fecha.isAfter(fin)) {
                continue;
            }
            AsistenciaActividadEstadoEnum estado = marcas.get(clase.slotId());
            if (estado == null) {
                if (!horarioIds.isEmpty() && clase.horarioId() != null && !horarioIds.contains(clase.horarioId())) {
                    continue;
                }
                estado = AsistenciaActividadEstadoEnum.AUSENTE;
            }
            int[] mes = c.mensual.computeIfAbsent(YearMonth.from(fecha), k -> new int[4]);
            c.clases++;
            mes[0]++;
            switch (estado) {
                case PRESENTE -> {
                    c.presentes++;
                    mes[1]++;
                    c.ultimaAsistencia = fecha;
                    consecutivas = 0;
                    clasesDesdeUltimoPresente = 0;
                }
                case AUSENTE -> {
                    c.ausentes++;
                    mes[2]++;
                    consecutivas++;
                    clasesDesdeUltimoPresente++;
                }
                case AUSENTE_JUSTIFICADO -> {
                    c.justificadas++;
                    mes[3]++;
                    clasesDesdeUltimoPresente++;
                }
            }
            if (conClases) {
                c.clases_.add(new AsistenciaAlumnoDetalleDTO.Clase(clase.inicio(), estado.name()));
            }
        }

        boolean activa = inscripcion.getStatus() != null && inscripcion.getStatus() == 0;
        LocalDate referencia = c.ultimaAsistencia != null ? c.ultimaAsistencia : inicio;
        long diasSinAsistir = ChronoUnit.DAYS.between(referencia, hoy);
        c.sinAsistir = activa && clasesDesdeUltimoPresente > 0 && diasSinAsistir >= umbrales.getDiasSinAsistencia();
        c.fila = fila(c, activa, consecutivas, diasSinAsistir, umbrales, hoy, profesores);
        return c;
    }

    private AsistenciaAlumnoFilaDTO fila(
            Calculo c,
            boolean activa,
            int consecutivas,
            long diasSinAsistir,
            AsistenciaUmbralesDTO umbrales,
            LocalDate hoy,
            Map<Long, List<Profesor>> profesores) {
        InscripcionActividad inscripcion = c.inscripcion;
        Member member = inscripcion.getMember();
        User user = member.getUser();
        Categoria categoria = member.getCategoria();

        AsistenciaAlumnoFilaDTO fila = new AsistenciaAlumnoFilaDTO();
        fila.setInscripcionUuid(inscripcion.getUuid());
        fila.setMemberUuid(member.getUuid());
        fila.setNombre(user != null ? user.getNombre() : null);
        fila.setApellido(user != null ? user.getApellido() : null);
        fila.setDni(user != null ? user.getDni() : null);
        fila.setImageUrl(user != null ? user.getImageUrl() : null);
        fila.setCategoria(categoria != null ? categoria.getNombre() : null);
        fila.setActividadUuid(inscripcion.getActividad().getUuid());
        fila.setActividadNombre(inscripcion.getActividad().getNombre());
        fila.setProfesores(nombres(profesores.getOrDefault(inscripcion.getActividad().getId(), List.of())));
        fila.setFechaInscripcion(inscripcion.getFechaInscripcion());
        fila.setFechaBaja(inscripcion.getFechaBaja());
        fila.setMotivoBaja(inscripcion.getMotivoBaja());
        fila.setActiva(activa);
        LocalDate finAntiguedad = inscripcion.getFechaBaja() != null ? inscripcion.getFechaBaja() : hoy;
        fila.setAntiguedadDias(Math.max(0, ChronoUnit.DAYS.between(inscripcion.getFechaInscripcion(), finAntiguedad)));
        fila.setAntiguedad(textoAntiguedad(inscripcion.getFechaInscripcion(), finAntiguedad));
        fila.setClases(c.clases);
        fila.setPresentes(c.presentes);
        fila.setAusentes(c.ausentes);
        fila.setJustificadas(c.justificadas);
        fila.setPorcentaje(porcentaje(c.presentes, c.clases));
        fila.setNivel(nivel(fila.getPorcentaje(), umbrales));
        fila.setUltimaAsistencia(c.ultimaAsistencia);
        fila.setAusenciasConsecutivas(consecutivas);

        if (activa) {
            if (c.sinAsistir) {
                fila.getAlertas().add(diasSinAsistir + " días sin asistir");
            }
            if (fila.getPorcentaje() != null && fila.getPorcentaje() < umbrales.getPorcentajeAlerta()) {
                fila.getAlertas().add("Asistencia por debajo del " + umbrales.getPorcentajeAlerta() + "%");
            }
            if (consecutivas >= umbrales.getAusenciasConsecutivas()) {
                fila.getAlertas().add(consecutivas + " ausencias seguidas");
            }
        }
        return fila;
    }

    private AsistenciaEstadisticasDTO.Resumen resumen(
            List<Calculo> calculos, Datos datos, Set<Long> actividadIds, Rango rango) {
        AsistenciaEstadisticasDTO.Resumen resumen = new AsistenciaEstadisticasDTO.Resumen();
        Set<Long> socios = new HashSet<>();
        Set<Long> activos = new HashSet<>();
        int clases = 0;
        for (Calculo c : calculos) {
            Long memberId = c.inscripcion.getMember().getId();
            socios.add(memberId);
            if (c.fila.isActiva()) {
                activos.add(memberId);
                if ("ROJO".equals(c.fila.getNivel())) {
                    resumen.setBajaAsistencia(resumen.getBajaAsistencia() + 1);
                }
                if (c.sinAsistir) {
                    resumen.setDejaronDeAsistir(resumen.getDejaronDeAsistir() + 1);
                }
                if (!c.fila.getAlertas().isEmpty()) {
                    resumen.setConAlertas(resumen.getConAlertas() + 1);
                }
            }
            clases += c.clases;
            resumen.setPresentes(resumen.getPresentes() + c.presentes);
            resumen.setAusentes(resumen.getAusentes() + c.ausentes);
            resumen.setJustificadas(resumen.getJustificadas() + c.justificadas);
        }
        resumen.setSociosInscriptos(socios.size());
        resumen.setAlumnosActivos(activos.size());
        resumen.setPromedioAsistencia(porcentaje(resumen.getPresentes(), clases));
        resumen.setClasesRealizadas((int) actividadIds.stream()
                .mapToLong(id -> datos.clases.getOrDefault(id, List.of()).size())
                .sum());
        return resumen;
    }

    private List<AsistenciaEstadisticasDTO.ActividadResumen> rankingActividades(
            List<Calculo> calculos, Datos datos, Rango rango, Map<Long, List<Profesor>> profesores) {
        Map<Long, List<Calculo>> porActividad = calculos.stream().collect(Collectors.groupingBy(
                c -> c.inscripcion.getActividad().getId(), LinkedHashMap::new, Collectors.toList()));
        List<AsistenciaEstadisticasDTO.ActividadResumen> ranking = new ArrayList<>();
        porActividad.forEach((actividadId, lista) -> {
            AsistenciaEstadisticasDTO.ActividadResumen item = new AsistenciaEstadisticasDTO.ActividadResumen();
            item.setActividadUuid(lista.get(0).inscripcion.getActividad().getUuid());
            item.setNombre(lista.get(0).inscripcion.getActividad().getNombre());
            item.setProfesores(nombres(profesores.getOrDefault(actividadId, List.of())));
            item.setInscriptos((int) lista.stream().map(c -> c.inscripcion.getMember().getId()).distinct().count());
            item.setClases(datos.clases.getOrDefault(actividadId, List.of()).size());
            item.setPromedioAsistencia(porcentaje(
                    lista.stream().mapToInt(c -> c.presentes).sum(),
                    lista.stream().mapToInt(c -> c.clases).sum()));
            ranking.add(item);
        });
        ranking.sort(Comparator.comparing(
                AsistenciaEstadisticasDTO.ActividadResumen::getPromedioAsistencia,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return ranking;
    }

    private List<AsistenciaEstadisticasDTO.ProfesorResumen> rankingProfesores(
            List<Calculo> calculos, Map<Long, List<Profesor>> profesores) {
        Map<Long, Profesor> profesorPorId = new LinkedHashMap<>();
        Map<Long, Set<Long>> actividadesPorProfesor = new HashMap<>();
        profesores.forEach((actividadId, lista) -> lista.forEach(p -> {
            profesorPorId.put(p.getId(), p);
            actividadesPorProfesor.computeIfAbsent(p.getId(), k -> new HashSet<>()).add(actividadId);
        }));

        List<AsistenciaEstadisticasDTO.ProfesorResumen> ranking = new ArrayList<>();
        profesorPorId.forEach((profesorId, profesor) -> {
            Set<Long> actividades = actividadesPorProfesor.get(profesorId);
            List<Calculo> propios = calculos.stream()
                    .filter(c -> actividades.contains(c.inscripcion.getActividad().getId()))
                    .toList();
            AsistenciaEstadisticasDTO.ProfesorResumen item = new AsistenciaEstadisticasDTO.ProfesorResumen();
            item.setProfesorUuid(profesor.getUuid());
            item.setNombre(nombre(profesor));
            item.setActividades(actividades.size());
            item.setAlumnos((int) propios.stream().map(c -> c.inscripcion.getMember().getId()).distinct().count());
            item.setPromedioAsistencia(porcentaje(
                    propios.stream().mapToInt(c -> c.presentes).sum(),
                    propios.stream().mapToInt(c -> c.clases).sum()));
            ranking.add(item);
        });
        ranking.sort(Comparator.comparing(
                AsistenciaEstadisticasDTO.ProfesorResumen::getPromedioAsistencia,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return ranking;
    }

    private List<AsistenciaMesDTO> evolucion(List<Calculo> calculos) {
        Map<YearMonth, int[]> total = new TreeMap<>();
        for (Calculo c : calculos) {
            c.mensual.forEach((mes, valores) -> {
                int[] acumulado = total.computeIfAbsent(mes, k -> new int[4]);
                for (int i = 0; i < 4; i++) {
                    acumulado[i] += valores[i];
                }
            });
        }
        List<AsistenciaMesDTO> meses = new ArrayList<>();
        total.forEach((mes, v) -> meses.add(new AsistenciaMesDTO(
                mes.toString(), v[0], v[1], v[2], v[3], porcentaje(v[1], v[0]))));
        return meses;
    }

    // ---------------------------------------------------------------- Trayectoria

    private TrayectoriaDeportivaDTO trayectoria(Company company, Member member, Set<Long> actividadesPermitidas) {
        LocalDate hoy = LocalDate.now();
        AsistenciaUmbralesDTO umbrales = umbralesService.obtener(company);
        List<InscripcionActividad> historial = inscripcionActividadRepository
                .findHistorialByMemberId(company.getId(), member.getId()).stream()
                .filter(i -> actividadesPermitidas == null || actividadesPermitidas.contains(i.getActividad().getId()))
                .toList();

        TrayectoriaDeportivaDTO dto = new TrayectoriaDeportivaDTO();
        User user = member.getUser();
        dto.setMemberUuid(member.getUuid());
        dto.setNombre(user != null ? user.getNombre() : null);
        dto.setApellido(user != null ? user.getApellido() : null);
        dto.setCategoriaActual(member.getCategoria() != null ? member.getCategoria().getNombre() : null);
        dto.setFechaAltaClub(member.getFechaAlta());
        if (member.getFechaAlta() != null) {
            dto.setAntiguedadClub(textoAntiguedad(member.getFechaAlta(), hoy));
        }
        if (historial.isEmpty()) {
            return dto;
        }

        Map<Long, List<InscripcionActividad>> porActividad = historial.stream().collect(Collectors.groupingBy(
                i -> i.getActividad().getId(), LinkedHashMap::new, Collectors.toList()));
        Rango rango = new Rango(INICIO_HISTORIA, hoy);
        Datos datos = cargarDatos(company.getId(), porActividad.keySet(), Set.of(member.getId()), rango);
        Map<Long, List<Profesor>> profesores = profesoresPorActividad(porActividad.keySet());

        porActividad.forEach((actividadId, periodos) -> {
            periodos = periodos.stream().sorted(Comparator.comparing(InscripcionActividad::getFechaInscripcion)).toList();
            TrayectoriaDeportivaDTO.ActividadTrayectoria item = new TrayectoriaDeportivaDTO.ActividadTrayectoria();
            InscripcionActividad primera = periodos.get(0);
            item.setActividadUuid(primera.getActividad().getUuid());
            item.setNombre(primera.getActividad().getNombre());
            item.setPrimeraInscripcion(primera.getFechaInscripcion());
            item.setProfesores(nombres(profesores.getOrDefault(actividadId, List.of())));
            item.setCategorias(primera.getActividad().getCategorias().stream()
                    .filter(Objects::nonNull).map(Categoria::getNombre).sorted().toList());

            long dias = 0;
            LocalDate finAnterior = null;
            for (InscripcionActividad periodo : periodos) {
                boolean activa = periodo.getStatus() != null && periodo.getStatus() == 0;
                LocalDate hasta = activa ? hoy : periodo.getFechaBaja();
                dias += Math.max(0, ChronoUnit.DAYS.between(periodo.getFechaInscripcion(), hasta));
                if (finAnterior != null && periodo.getFechaInscripcion().isAfter(finAnterior)) {
                    item.setInterrupciones(item.getInterrupciones() + 1);
                }
                finAnterior = hasta;
                item.setActiva(item.isActiva() || activa);
                if (!activa && (item.getUltimaBaja() == null || periodo.getFechaBaja().isAfter(item.getUltimaBaja()))) {
                    item.setUltimaBaja(periodo.getFechaBaja());
                }

                Calculo c = computar(periodo, datos, rango, umbrales, hoy, false, profesores);
                item.setClases(item.getClases() + c.clases);
                item.setPresentes(item.getPresentes() + c.presentes);
                item.setAusentes(item.getAusentes() + c.ausentes);
                item.setJustificadas(item.getJustificadas() + c.justificadas);

                TrayectoriaDeportivaDTO.Periodo p = new TrayectoriaDeportivaDTO.Periodo();
                p.setInscripcionUuid(periodo.getUuid());
                p.setDesde(periodo.getFechaInscripcion());
                p.setHasta(activa ? null : periodo.getFechaBaja());
                p.setActiva(activa);
                p.setMotivoBaja(periodo.getMotivoBaja());
                p.setDuracion(textoAntiguedad(periodo.getFechaInscripcion(), hasta));
                item.getPeriodos().add(p);

                boolean esPrimera = periodo == primera;
                dto.getHitos().add(hito(periodo.getFechaInscripcion(), esPrimera ? "INSCRIPCION" : "REINCORPORACION",
                        item.getNombre(), esPrimera ? "Se inscribió" : "Volvió a inscribirse"));
                if (!activa) {
                    dto.getHitos().add(hito(periodo.getFechaBaja(), "BAJA", item.getNombre(),
                            periodo.getMotivoBaja() != null ? periodo.getMotivoBaja() : "Se dio de baja"));
                }
            }
            item.setReinscripciones(periodos.size() - 1);
            item.setDiasActivos(dias);
            item.setTiempoActivo(textoAntiguedad(primera.getFechaInscripcion(), primera.getFechaInscripcion().plusDays(dias)));
            item.setAntiguedad(textoAntiguedad(primera.getFechaInscripcion(),
                    item.isActiva() ? hoy : item.getUltimaBaja()));
            item.setPorcentaje(porcentaje(item.getPresentes(), item.getClases()));
            item.setNivel(nivel(item.getPorcentaje(), umbrales));
            dto.getActividades().add(item);
        });

        dto.getActividades().sort(Comparator
                .comparing(TrayectoriaDeportivaDTO.ActividadTrayectoria::isActiva).reversed()
                .thenComparing(TrayectoriaDeportivaDTO.ActividadTrayectoria::getPrimeraInscripcion));
        dto.getHitos().sort(Comparator.comparing(TrayectoriaDeportivaDTO.Hito::getFecha).reversed());
        dto.setActividadesActuales((int) dto.getActividades().stream().filter(TrayectoriaDeportivaDTO.ActividadTrayectoria::isActiva).count());
        dto.setActividadesAnteriores(dto.getActividades().size() - dto.getActividadesActuales());
        return dto;
    }

    // ---------------------------------------------------------------- Carga de datos

    private Datos cargarDatos(Long companyId, Collection<Long> actividadIds, Set<Long> memberIds, Rango rango) {
        Datos datos = new Datos();
        if (actividadIds.isEmpty()) {
            return datos;
        }
        LocalDateTime desde = rango.desde().atStartOfDay();
        LocalDateTime hasta = rango.hasta().plusDays(1).atStartOfDay();
        for (Object[] fila : slotRepository.findClasesRealizadas(companyId, actividadIds, desde, hasta)) {
            datos.clases.computeIfAbsent((Long) fila[1], k -> new ArrayList<>())
                    .add(new ClaseRealizada((Long) fila[0], (Long) fila[2], (LocalDateTime) fila[3]));
        }
        List<Object[]> marcas = memberIds == null
                ? asistenciaActividadRepository.findMarcasByActividadIdIn(companyId, actividadIds, rango.desde(), rango.hasta())
                : asistenciaActividadRepository.findMarcasByActividadIdInAndMemberIdIn(
                        companyId, actividadIds, memberIds, rango.desde(), rango.hasta());
        for (Object[] fila : marcas) {
            datos.marcas.computeIfAbsent((Long) fila[1], k -> new HashMap<>())
                    .put((Long) fila[0], (AsistenciaActividadEstadoEnum) fila[2]);
        }
        return datos;
    }

    private Map<Long, List<Profesor>> profesoresPorActividad(Collection<Long> actividadIds) {
        if (actividadIds.isEmpty()) {
            return Map.of();
        }
        List<ActividadProfesor> asignaciones = actividadProfesorRepository.findActivasByActividadIdIn(new ArrayList<>(actividadIds));
        if (asignaciones.isEmpty()) {
            return Map.of();
        }
        Map<Long, Profesor> porId = profesorRepository.findAllWithUserByIdIn(
                        asignaciones.stream().map(ActividadProfesor::getProfesorId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Profesor::getId, p -> p));
        Map<Long, List<Profesor>> resultado = new HashMap<>();
        for (ActividadProfesor asignacion : asignaciones) {
            Profesor profesor = porId.get(asignacion.getProfesorId());
            if (profesor != null) {
                resultado.computeIfAbsent(asignacion.getActividadId(), k -> new ArrayList<>()).add(profesor);
            }
        }
        return resultado;
    }

    // ---------------------------------------------------------------- Alcance y validaciones

    private Set<Long> alcanceInstitucional(AsistenciaEstadisticasRequestDTO request, Long companyId) {
        Set<Long> ids = tieneTexto(request.getActividadUuid())
                ? new LinkedHashSet<>(Set.of(resolverActividadId(request.getActividadUuid(), companyId)))
                : new LinkedHashSet<>(actividadRepository.findIdsActivasByCompanyId(companyId));
        if (tieneTexto(request.getProfesorUuid())) {
            Profesor profesor = profesorRepository.findByUuidAndCompanyId(request.getProfesorUuid(), companyId)
                    .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Profesor no encontrado",
                            "AsistenciaEstadisticasService | profesorUuid: " + request.getProfesorUuid()));
            ids.retainAll(new HashSet<>(actividadProfesorRepository.findActividadIdsByProfesorId(profesor.getId())));
        }
        return ids;
    }

    private Set<Long> actividadesDelProfesor(Long companyId) {
        Profesor profesor = profesorAuthService.requirePortalEligible(
                profesorAuthService.getCurrentProfesorOrThrow(companyId));
        Set<Long> propias = new LinkedHashSet<>(actividadProfesorRepository.findActividadIdsByProfesorId(profesor.getId()));
        // ! Solo actividades vigentes del club actual
        propias.retainAll(new HashSet<>(actividadRepository.findIdsActivasByCompanyId(companyId)));
        return propias;
    }

    private Long resolverActividadId(String actividadUuid, Long companyId) {
        return actividadRepository.findByUuidAndCompany_Id(actividadUuid, companyId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Actividad no encontrada",
                        "AsistenciaEstadisticasService | actividadUuid: " + actividadUuid))
                .getId();
    }

    private InscripcionActividad resolverInscripcion(String inscripcionUuid, Long companyId) {
        return inscripcionActividadRepository.findPeriodoByUuid(inscripcionUuid, companyId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Inscripción no encontrada",
                        "AsistenciaEstadisticasService | inscripcionUuid: " + inscripcionUuid));
    }

    private Member resolverSocio(String memberUuid, Long companyId) {
        return tieneTexto(memberUuid)
                ? memberAuthService.resolveMemberOfFamilyOrThrow(memberUuid, companyId)
                : memberAuthService.getCurrentMemberOrThrow(companyId);
    }

    private List<InscripcionActividad> filtrar(List<InscripcionActividad> inscripciones, AsistenciaEstadisticasRequestDTO request) {
        String busqueda = tieneTexto(request.getSearch()) ? normalizar(request.getSearch()) : null;
        return inscripciones.stream()
                .filter(i -> !tieneTexto(request.getMemberUuid()) || request.getMemberUuid().equals(i.getMember().getUuid()))
                .filter(i -> !tieneTexto(request.getCategoriaUuid())
                        || (i.getMember().getCategoria() != null
                        && request.getCategoriaUuid().equals(i.getMember().getCategoria().getUuid())))
                .filter(i -> busqueda == null || coincide(i.getMember(), busqueda))
                .toList();
    }

    private boolean coincide(Member member, String busqueda) {
        User user = member.getUser();
        if (user == null) {
            return false;
        }
        String texto = normalizar(String.join(" ",
                Objects.toString(user.getNombre(), ""),
                Objects.toString(user.getApellido(), ""),
                Objects.toString(user.getDni(), "")));
        for (String palabra : busqueda.split("\\s+")) {
            if (!texto.contains(palabra)) {
                return false;
            }
        }
        return true;
    }

    private static Comparator<AsistenciaAlumnoFilaDTO> orden(String orden) {
        Comparator<AsistenciaAlumnoFilaDTO> porNombre = Comparator
                .comparing((AsistenciaAlumnoFilaDTO f) -> Objects.toString(f.getApellido(), ""), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(f -> Objects.toString(f.getNombre(), ""), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(f -> Objects.toString(f.getActividadNombre(), ""));
        if ("ASISTENCIA_ASC".equals(orden)) {
            return Comparator.comparing(AsistenciaAlumnoFilaDTO::getPorcentaje, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(porNombre);
        }
        if ("ASISTENCIA_DESC".equals(orden)) {
            return Comparator.comparing(AsistenciaAlumnoFilaDTO::getPorcentaje, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(porNombre);
        }
        return porNombre;
    }

    private static Rango rango(LocalDate desde, LocalDate hasta, LocalDate hoy) {
        LocalDate fin = hasta == null || hasta.isAfter(hoy) ? hoy : hasta;
        LocalDate inicio = desde == null ? INICIO_HISTORIA : desde;
        if (inicio.isAfter(fin)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "La fecha desde no puede ser posterior a la fecha hasta",
                    "AsistenciaEstadisticasService | desde: " + desde + " hasta: " + hasta);
        }
        return new Rango(inicio, fin);
    }

    private static CustomException sinAcceso(String detalle) {
        return new CustomException(HttpStatus.FORBIDDEN, "Este alumno o actividad no está asignado a vos",
                "AsistenciaEstadisticasService | sin asignacion | " + detalle);
    }

    // ---------------------------------------------------------------- Utilidades

    static Double porcentaje(int presentes, int clases) {
        if (clases <= 0) {
            return null;
        }
        return Math.round(presentes * 1000.0 / clases) / 10.0;
    }

    static String nivel(Double porcentaje, AsistenciaUmbralesDTO umbrales) {
        if (porcentaje == null) {
            return "SIN_DATOS";
        }
        if (porcentaje >= umbrales.getVerde()) {
            return "VERDE";
        }
        return porcentaje >= umbrales.getAmarillo() ? "AMARILLO" : "ROJO";
    }

    /** "2 años y 8 meses", "1 año", "3 meses" o "12 días". */
    static String textoAntiguedad(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            return "0 días";
        }
        Period periodo = Period.between(desde, hasta);
        int anios = periodo.getYears();
        int meses = periodo.getMonths();
        if (anios == 0 && meses == 0) {
            int dias = periodo.getDays();
            return dias + (dias == 1 ? " día" : " días");
        }
        String textoAnios = anios == 1 ? "1 año" : anios + " años";
        String textoMeses = meses == 1 ? "1 mes" : meses + " meses";
        if (anios == 0) {
            return textoMeses;
        }
        return meses == 0 ? textoAnios : textoAnios + " y " + textoMeses;
    }

    private static TrayectoriaDeportivaDTO.Hito hito(LocalDate fecha, String tipo, String actividad, String detalle) {
        TrayectoriaDeportivaDTO.Hito hito = new TrayectoriaDeportivaDTO.Hito();
        hito.setFecha(fecha);
        hito.setTipo(tipo);
        hito.setActividad(actividad);
        hito.setDetalle(detalle);
        return hito;
    }

    private static List<String> nombres(List<Profesor> profesores) {
        return profesores.stream().map(AsistenciaEstadisticasService::nombre).sorted().toList();
    }

    private static String nombre(Profesor profesor) {
        User user = profesor.getUser();
        return user == null ? "" : (Objects.toString(user.getNombre(), "") + " " + Objects.toString(user.getApellido(), "")).trim();
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto.toLowerCase().trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static LocalDate max(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDate min(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }

    private record Rango(LocalDate desde, LocalDate hasta) { }

    private record ClaseRealizada(Long slotId, Long horarioId, LocalDateTime inicio) { }

    private static final class Datos {
        /** actividadId -> clases realizadas ordenadas por inicio. */
        private final Map<Long, List<ClaseRealizada>> clases = new HashMap<>();
        /** memberId -> slotId -> estado registrado. */
        private final Map<Long, Map<Long, AsistenciaActividadEstadoEnum>> marcas = new HashMap<>();
    }

    private static final class Calculo {
        private InscripcionActividad inscripcion;
        private int clases;
        private int presentes;
        private int ausentes;
        private int justificadas;
        private LocalDate ultimaAsistencia;
        private boolean sinAsistir;
        private final Map<YearMonth, int[]> mensual = new TreeMap<>();
        private final List<AsistenciaAlumnoDetalleDTO.Clase> clases_ = new ArrayList<>();
        private AsistenciaAlumnoFilaDTO fila;
    }
}
