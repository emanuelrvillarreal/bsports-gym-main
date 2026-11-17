package com.blue.open.Code.Service.Equipamiento;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.EquipamientoDTO;
import com.blue.open.Code.Entity.DTO.MovimientoDetalleDTO;
import com.blue.open.Code.Entity.DTO.MovimientoManualCommand;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Equipamiento;
import com.blue.open.Code.Entity.Db.EquipamientoMantenimiento;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.EquipamientoEstadoEnum;
import com.blue.open.Code.Entity.Enums.MantenimientoTipoEnum;
import com.blue.open.Code.Entity.Enums.MovimientoTipoEnum;
import com.blue.open.Code.Repository.EquipamientoMantenimientoRepository;
import com.blue.open.Code.Repository.EquipamientoRepository;
import com.blue.open.Code.Repository.MovimientoRepository;
import com.blue.open.Code.Service.Tesoreria.MovimientoManualService;
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

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Inventario de equipamiento e historial de mantenimientos, siempre del club del header.
 * Solo ADMIN (EQUIPMENT_MANAGE). El costo de un mantenimiento puede registrarse en el mismo paso como
 * egreso de tesoreria (reutiliza MovimientoManualService, que exige el permiso de egresos), y asi
 * aparece en los reportes economicos.
 */
@Service
public class EquipamientoService {

    static final int DIAS_AVISO = 30;

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private AuthService authService;

    @Autowired
    private EquipamientoRepository repository;

    @Autowired
    private EquipamientoMantenimientoRepository mantenimientoRepository;

    @Autowired
    private MovimientoManualService movimientoManualService;

    @Autowired
    private MovimientoRepository movimientoRepository;

    /** Fecha del ultimo mantenimiento, proximo previsto (del ultimo) y costo acumulado de un equipo. */
    private record Estado(LocalDate ultimo, LocalDate proximo, BigDecimal costo) { }

    // ---------------------------------------------------------------- Listado y resumen

    @Transactional(readOnly = true)
    public Page<EquipamientoDTO.Fila> listar(EquipamientoDTO.PaginateRequest request) {
        Company company = company();
        StringBuilder jpql = new StringBuilder("SELECT e FROM Equipamiento e WHERE e.company.id = :c AND e.status = 0");
        Map<String, Object> params = new HashMap<>();
        params.put("c", company.getId());
        if (!request.isIncluirBajas() && request.getEstado() != EquipamientoEstadoEnum.BAJA) {
            jpql.append(" AND e.estado <> com.blue.open.Code.Entity.Enums.EquipamientoEstadoEnum.BAJA");
        }
        if (request.getEstado() != null) {
            jpql.append(" AND e.estado = :estado");
            params.put("estado", request.getEstado());
        }
        if (tieneTexto(request.getSearch())) {
            jpql.append(" AND (LOWER(e.nombre) LIKE :s OR LOWER(e.codigo) LIKE :s OR LOWER(e.tipo) LIKE :s OR LOWER(e.ubicacion) LIKE :s"
                    + " OR LOWER(e.marca) LIKE :s)");
            params.put("s", like(request.getSearch()));
        }
        if (tieneTexto(request.getDisciplina())) {
            jpql.append(" AND LOWER(e.disciplina) LIKE :d");
            params.put("d", like(request.getDisciplina()));
        }
        jpql.append(" ORDER BY LOWER(e.nombre)");
        TypedQuery<Equipamiento> query = em.createQuery(jpql.toString(), Equipamiento.class);
        params.forEach(query::setParameter);

        Map<Long, Estado> estados = estados(company.getId());
        LocalDate hoy = LocalDate.now();
        List<EquipamientoDTO.Fila> filas = new ArrayList<>();
        for (Equipamiento e : query.getResultList()) {
            Estado estado = estados.getOrDefault(e.getId(), new Estado(null, null, BigDecimal.ZERO));
            String alerta = e.getEstado() == EquipamientoEstadoEnum.BAJA ? null : alerta(estado.proximo(), hoy);
            if (request.getAlerta() != null && !request.getAlerta().equals(alerta)) {
                continue;
            }
            EquipamientoDTO.Fila fila = new EquipamientoDTO.Fila();
            fila.setUuid(e.getUuid());
            fila.setNombre(e.getNombre());
            fila.setCodigo(e.getCodigo());
            fila.setTipo(e.getTipo());
            fila.setDisciplina(e.getDisciplina());
            fila.setMarcaModelo(String.join(" ", Objects.toString(e.getMarca(), ""), Objects.toString(e.getModelo(), "")).trim());
            fila.setCantidad(e.getCantidad());
            fila.setEstado(e.getEstado());
            fila.setUbicacion(e.getUbicacion());
            fila.setUltimoMantenimiento(estado.ultimo());
            fila.setProximoMantenimiento(estado.proximo());
            fila.setAlerta(alerta);
            fila.setCostoMantenimientos(estado.costo());
            filas.add(fila);
        }
        PageRequest pageable = PageRequest.of(request.getPage() - 1, request.getSize());
        int desde = Math.min((int) pageable.getOffset(), filas.size());
        int hasta = Math.min(desde + pageable.getPageSize(), filas.size());
        return new PageImpl<>(filas.subList(desde, hasta), pageable, filas.size());
    }

    @Transactional(readOnly = true)
    public EquipamientoDTO.Resumen resumen() {
        Company company = company();
        EquipamientoDTO.Resumen resumen = new EquipamientoDTO.Resumen();
        Map<Long, Estado> estados = estados(company.getId());
        LocalDate hoy = LocalDate.now();
        for (Object[] f : em.createQuery("SELECT e.id, e.cantidad, e.estado FROM Equipamiento e WHERE e.company.id = :c AND e.status = 0"
                        + " AND e.estado <> com.blue.open.Code.Entity.Enums.EquipamientoEstadoEnum.BAJA", Object[].class)
                .setParameter("c", company.getId()).getResultList()) {
            resumen.setItems(resumen.getItems() + 1);
            resumen.setUnidades(resumen.getUnidades() + ((Number) f[1]).longValue());
            if (f[2] == EquipamientoEstadoEnum.EN_REPARACION) resumen.setEnReparacion(resumen.getEnReparacion() + 1);
            if (f[2] == EquipamientoEstadoEnum.FUERA_DE_SERVICIO) resumen.setFueraDeServicio(resumen.getFueraDeServicio() + 1);
            Estado estado = estados.get((Long) f[0]);
            String alerta = estado == null ? null : alerta(estado.proximo(), hoy);
            if ("VENCIDO".equals(alerta)) resumen.setMantenimientosVencidos(resumen.getMantenimientosVencidos() + 1);
            if ("PROXIMO".equals(alerta)) resumen.setMantenimientosProximos(resumen.getMantenimientosProximos() + 1);
        }
        BigDecimal costoAnio = em.createQuery("SELECT COALESCE(SUM(m.costo), 0) FROM EquipamientoMantenimiento m"
                        + " WHERE m.equipamiento.company.id = :c AND m.status = 0 AND m.fecha >= :desde", BigDecimal.class)
                .setParameter("c", company.getId())
                .setParameter("desde", hoy.withDayOfYear(1))
                .getSingleResult();
        resumen.setCostoAnio(costoAnio);
        return resumen;
    }

    // ---------------------------------------------------------------- ABM

    @Transactional(readOnly = true)
    public EquipamientoDTO.Detalle obtener(String uuid) {
        return detalle(equipamiento(uuid, company()));
    }

    @Transactional
    public EquipamientoDTO.Detalle crear(EquipamientoDTO.Command command) {
        Company company = company();
        Equipamiento e = new Equipamiento();
        e.setCompany(company);
        aplicar(e, command, company);
        return detalle(repository.save(e));
    }

    @Transactional
    public EquipamientoDTO.Detalle editar(String uuid, EquipamientoDTO.Command command) {
        Company company = company();
        Equipamiento e = equipamiento(uuid, company);
        aplicar(e, command, company);
        return detalle(repository.save(e));
    }

    /** Elimina un alta hecha por error (baja logica). Para un equipo que ya no se usa, conviene el estado BAJA. */
    @Transactional
    public void eliminar(String uuid) {
        Equipamiento e = equipamiento(uuid, company());
        e.setStatus(-1);
        e.setDateDeleted(new Timestamp(System.currentTimeMillis()));
        repository.save(e);
    }

    // ---------------------------------------------------------------- Mantenimientos

    @Transactional
    public EquipamientoDTO.Detalle registrarMantenimiento(String uuid, EquipamientoDTO.MantenimientoCommand c) {
        Company company = company();
        Equipamiento equipo = equipamiento(uuid, company);
        if (c.getFecha().isAfter(LocalDate.now())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "La fecha del mantenimiento no puede ser futura; usá \"Próximo mantenimiento\" para planificarlo",
                    "EquipamientoService | fecha futura");
        }
        if (c.getProximoMantenimiento() != null && !c.getProximoMantenimiento().isAfter(c.getFecha())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "El próximo mantenimiento tiene que ser posterior",
                    "EquipamientoService | proximo invalido");
        }
        EquipamientoMantenimiento m = new EquipamientoMantenimiento();
        m.setEquipamiento(equipo);
        m.setTipo(c.getTipo());
        m.setFecha(c.getFecha());
        m.setResponsable(limpio(c.getResponsable()));
        m.setDescripcion(c.getDescripcion().trim());
        m.setCosto(c.getCosto());
        m.setProximoMantenimiento(c.getProximoMantenimiento());
        m.setRegistradoPor(authService.getActualOrThrow());

        if (c.isRegistrarEgreso()) {
            if (c.getCosto() == null || c.getCosto().signum() <= 0) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "Para registrar el egreso cargá el costo",
                        "EquipamientoService | egreso sin costo");
            }
            MovimientoManualCommand egreso = new MovimientoManualCommand();
            egreso.setTipo(MovimientoTipoEnum.EGRESO);
            egreso.setMonto(c.getCosto());
            egreso.setFecha(c.getFecha());
            egreso.setConcepto(recortar(textoTipo(c.getTipo()) + " de equipamiento: " + equipo.getNombre(), 255));
            egreso.setObservaciones(c.getDescripcion().trim());
            // * Valida TREASURY_EXPENSE y deja el egreso en el libro de caja (aparece en los reportes)
            MovimientoDetalleDTO movimiento = movimientoManualService.registrar(egreso);
            m.setMovimiento(movimientoRepository.findFirstByUuid(movimiento.getMovimientoUuid()));
        }
        mantenimientoRepository.save(m);

        if (c.getEstadoResultante() != null && c.getEstadoResultante() != equipo.getEstado()) {
            equipo.setEstado(c.getEstadoResultante());
            repository.save(equipo);
        }
        return detalle(equipo);
    }

    @Transactional
    public EquipamientoDTO.Detalle eliminarMantenimiento(String uuid) {
        Company company = company();
        EquipamientoMantenimiento m = em.createQuery("SELECT m FROM EquipamientoMantenimiento m JOIN FETCH m.equipamiento e"
                                + " WHERE m.uuid = :u AND e.company.id = :c AND m.status = 0", EquipamientoMantenimiento.class)
                .setParameter("u", uuid).setParameter("c", company.getId())
                .getResultStream().findFirst()
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Mantenimiento no encontrado",
                        "EquipamientoService | mantenimientoUuid: " + uuid));
        if (m.getMovimiento() != null) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "Este mantenimiento generó un egreso en Tesorería. Anulá ese egreso desde Ingresos antes de eliminarlo.",
                    "EquipamientoService | mantenimiento con movimiento: " + uuid);
        }
        m.setStatus(-1);
        m.setDateDeleted(new Timestamp(System.currentTimeMillis()));
        mantenimientoRepository.save(m);
        return detalle(m.getEquipamiento());
    }

    // ---------------------------------------------------------------- Apoyo

    private void aplicar(Equipamiento e, EquipamientoDTO.Command c, Company company) {
        String codigo = limpio(c.getCodigo());
        if (codigo != null) {
            TypedQuery<Long> query = em.createQuery("SELECT COUNT(e.id) FROM Equipamiento e WHERE e.company.id = :c AND e.status = 0"
                            + " AND LOWER(e.codigo) = :cod" + (e.getId() != null ? " AND e.id <> :id" : ""), Long.class)
                    .setParameter("c", company.getId())
                    .setParameter("cod", codigo.toLowerCase(Locale.ROOT));
            if (e.getId() != null) {
                query.setParameter("id", e.getId());
            }
            Long repetidos = query.getSingleResult();
            if (repetidos > 0) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "Ya hay un equipamiento con el código " + codigo,
                        "EquipamientoService | codigo repetido");
            }
        }
        if (c.getFechaAdquisicion() != null && c.getFechaAdquisicion().isAfter(LocalDate.now())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "La fecha de adquisición no puede ser futura",
                    "EquipamientoService | adquisicion futura");
        }
        e.setNombre(c.getNombre().trim());
        e.setCodigo(codigo);
        e.setTipo(limpio(c.getTipo()));
        e.setDisciplina(limpio(c.getDisciplina()));
        e.setMarca(limpio(c.getMarca()));
        e.setModelo(limpio(c.getModelo()));
        e.setCantidad(c.getCantidad());
        e.setEstado(c.getEstado());
        e.setUbicacion(limpio(c.getUbicacion()));
        e.setFechaAdquisicion(c.getFechaAdquisicion());
        e.setObservaciones(limpio(c.getObservaciones()));
    }

    private EquipamientoDTO.Detalle detalle(Equipamiento e) {
        EquipamientoDTO.Detalle dto = new EquipamientoDTO.Detalle();
        dto.setUuid(e.getUuid());
        dto.setNombre(e.getNombre());
        dto.setCodigo(e.getCodigo());
        dto.setTipo(e.getTipo());
        dto.setDisciplina(e.getDisciplina());
        dto.setMarca(e.getMarca());
        dto.setModelo(e.getModelo());
        dto.setCantidad(e.getCantidad());
        dto.setEstado(e.getEstado());
        dto.setUbicacion(e.getUbicacion());
        dto.setFechaAdquisicion(e.getFechaAdquisicion());
        dto.setObservaciones(e.getObservaciones());
        List<EquipamientoMantenimiento> mantenimientos = em.createQuery(
                        "SELECT m FROM EquipamientoMantenimiento m LEFT JOIN FETCH m.registradoPor LEFT JOIN FETCH m.movimiento"
                                + " WHERE m.equipamiento.id = :e AND m.status = 0 ORDER BY m.fecha DESC, m.id DESC", EquipamientoMantenimiento.class)
                .setParameter("e", e.getId()).getResultList();
        BigDecimal costo = BigDecimal.ZERO;
        for (EquipamientoMantenimiento m : mantenimientos) {
            EquipamientoDTO.Mantenimiento item = new EquipamientoDTO.Mantenimiento();
            item.setUuid(m.getUuid());
            item.setTipo(m.getTipo());
            item.setFecha(m.getFecha());
            item.setResponsable(m.getResponsable());
            item.setDescripcion(m.getDescripcion());
            item.setCosto(m.getCosto());
            item.setProximoMantenimiento(m.getProximoMantenimiento());
            User u = m.getRegistradoPor();
            item.setRegistradoPor(u != null ? (Objects.toString(u.getNombre(), "") + " " + Objects.toString(u.getApellido(), "")).trim() : null);
            item.setMovimientoUuid(m.getMovimiento() != null ? m.getMovimiento().getUuid() : null);
            if (m.getCosto() != null) costo = costo.add(m.getCosto());
            dto.getMantenimientos().add(item);
        }
        dto.setCostoMantenimientos(costo);
        dto.setProximoMantenimiento(mantenimientos.isEmpty() ? null : mantenimientos.get(0).getProximoMantenimiento());
        return dto;
    }

    /** Por equipo: fecha del ultimo mantenimiento, proximo previsto por ese ultimo y costo total. */
    private Map<Long, Estado> estados(Long companyId) {
        Map<Long, Estado> estados = new HashMap<>();
        for (Object[] f : em.createQuery("SELECT m.equipamiento.id, m.fecha, m.proximoMantenimiento, m.costo FROM EquipamientoMantenimiento m"
                        + " WHERE m.equipamiento.company.id = :c AND m.status = 0 ORDER BY m.fecha, m.id", Object[].class)
                .setParameter("c", companyId).getResultList()) {
            Long id = (Long) f[0];
            Estado anterior = estados.get(id);
            BigDecimal costo = (anterior != null ? anterior.costo() : BigDecimal.ZERO)
                    .add(f[3] != null ? (BigDecimal) f[3] : BigDecimal.ZERO);
            estados.put(id, new Estado((LocalDate) f[1], (LocalDate) f[2], costo));
        }
        return estados;
    }

    static String alerta(LocalDate proximo, LocalDate hoy) {
        if (proximo == null) return null;
        if (proximo.isBefore(hoy)) return "VENCIDO";
        return proximo.isAfter(hoy.plusDays(DIAS_AVISO)) ? null : "PROXIMO";
    }

    private Equipamiento equipamiento(String uuid, Company company) {
        return em.createQuery("SELECT e FROM Equipamiento e WHERE e.uuid = :u AND e.company.id = :c AND e.status = 0", Equipamiento.class)
                .setParameter("u", uuid).setParameter("c", company.getId())
                .getResultStream().findFirst()
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Equipamiento no encontrado",
                        "EquipamientoService | uuid: " + uuid));
    }

    private Company company() {
        authService.valid(List.of(PermissionEnum.EQUIPMENT_MANAGE));
        return authService.getCurrentCompanyOrThrow();
    }

    private static String textoTipo(MantenimientoTipoEnum tipo) {
        return switch (tipo) {
            case PREVENTIVO -> "Mantenimiento preventivo";
            case REPARACION -> "Reparación";
            case REVISION -> "Revisión";
        };
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

    private static String recortar(String texto, int max) {
        return texto.length() <= max ? texto : texto.substring(0, max - 1) + "…";
    }
}
