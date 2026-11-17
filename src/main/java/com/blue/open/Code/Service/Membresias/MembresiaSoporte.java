package com.blue.open.Code.Service.Membresias;

import com.blue.open.Code.Entity.DTO.MembresiaDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Cuota;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.Membresia;
import com.blue.open.Code.Entity.Db.MembresiaAcceso;
import com.blue.open.Code.Entity.Db.MembresiaCongelamiento;
import com.blue.open.Code.Entity.Db.Movimiento;
import com.blue.open.Code.Entity.Db.Pase;
import com.blue.open.Code.Entity.Enums.CuotaEstadoEnum;
import com.blue.open.Code.Entity.Enums.DiaSemanaEnum;
import com.blue.open.Code.Entity.Enums.MembresiaEstadoEnum;
import com.blue.open.Code.Entity.Enums.MembresiaSituacionEnum;
import com.blue.open.Code.Entity.Enums.MovimientoEstadoEnum;
import com.blue.open.Code.Entity.Enums.MovimientoTipoEnum;
import com.blue.open.Code.Entity.Db.Configuration;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Code.Repository.ConfigurationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Consultas y armado de respuestas compartidos por los servicios de pases y membresías. */
@Component
public class MembresiaSoporte {

    static final String CONFIG = "membresias.config";
    private static final ZoneId ZONA = ZoneId.of("America/Argentina/Buenos_Aires");

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private ConfigurationRepository configurationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /** Fecha del club (Argentina), no la del servidor: evita corrimientos de un día. */
    public LocalDate hoy() {
        return LocalDate.now(ZONA);
    }

    public java.time.LocalDateTime ahora() {
        return java.time.LocalDateTime.now(ZONA);
    }

    // ---------------------------------------------------------------- Búsquedas por uuid (siempre del club)

    public <T> T porUuid(Class<T> tipo, String uuid, Long companyId, String nombre) {
        if (uuid == null || uuid.isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, nombre + " requerido", "MembresiaSoporte | uuid vacío");
        }
        List<T> lista = em.createQuery("SELECT e FROM " + tipo.getSimpleName()
                        + " e WHERE e.uuid = :uuid AND e.company.id = :companyId AND e.status = 0", tipo)
                .setParameter("uuid", uuid.trim()).setParameter("companyId", companyId).getResultList();
        if (lista.isEmpty()) {
            throw new CustomException(HttpStatus.NOT_FOUND, nombre + " no encontrado",
                    "MembresiaSoporte | " + tipo.getSimpleName() + " uuid: " + uuid);
        }
        return lista.get(0);
    }

    public <T> List<T> porUuids(Class<T> tipo, Collection<String> uuids, Long companyId, String nombre) {
        List<T> resultado = new ArrayList<>();
        if (uuids == null) return resultado;
        for (String uuid : new HashSet<>(uuids)) {
            if (uuid != null && !uuid.isBlank()) resultado.add(porUuid(tipo, uuid, companyId, nombre));
        }
        return resultado;
    }

    public Membresia membresia(String uuid, Long companyId) {
        return porUuid(Membresia.class, uuid, companyId, "Membresía");
    }

    public Member socio(String uuid, Long companyId) {
        List<Member> lista = em.createQuery("""
                        SELECT m FROM Member m JOIN FETCH m.user LEFT JOIN FETCH m.titular
                        WHERE m.uuid = :uuid AND m.company.id = :companyId AND m.status = 0""", Member.class)
                .setParameter("uuid", uuid).setParameter("companyId", companyId).getResultList();
        if (lista.isEmpty()) {
            throw new CustomException(HttpStatus.NOT_FOUND, "Socio no encontrado", "MembresiaSoporte | memberUuid: " + uuid);
        }
        return lista.get(0);
    }

    /** Titular más familiares activos del grupo del socio. */
    public List<Member> grupo(Member socio) {
        Member titular = socio.isTitular() ? socio : socio.getTitular();
        return em.createQuery("""
                        SELECT m FROM Member m JOIN FETCH m.user
                        WHERE m.company.id = :companyId AND m.status = 0
                          AND (m.id = :titularId OR m.titular.id = :titularId)
                        ORDER BY CASE WHEN m.id = :titularId THEN 0 ELSE 1 END, m.id""", Member.class)
                .setParameter("companyId", titular.getCompany().getId()).setParameter("titularId", titular.getId())
                .getResultList();
    }

    /** Membresías (no borradas) donde el socio es quien contrató o integrante. */
    public List<Membresia> deSocio(Long memberId, Long companyId) {
        return em.createQuery("""
                        SELECT DISTINCT mb FROM Membresia mb JOIN FETCH mb.pase LEFT JOIN mb.integrantes i
                        WHERE mb.company.id = :companyId AND mb.status = 0
                          AND (mb.member.id = :memberId OR i.id = :memberId)
                        ORDER BY mb.fechaInicio DESC, mb.id DESC""", Membresia.class)
                .setParameter("companyId", companyId).setParameter("memberId", memberId).getResultList();
    }

    public List<Membresia> delClub(Long companyId) {
        return em.createQuery("""
                        SELECT mb FROM Membresia mb JOIN FETCH mb.pase JOIN FETCH mb.member m JOIN FETCH m.user
                        WHERE mb.company.id = :companyId AND mb.status = 0
                        ORDER BY mb.fechaInicio DESC, mb.id DESC""", Membresia.class)
                .setParameter("companyId", companyId).getResultList();
    }

    // ---------------------------------------------------------------- Cobro (la cuota de la membresía)

    public Map<Long, Cuota> cuotas(Collection<Membresia> membresias) {
        Map<Long, Cuota> resultado = new HashMap<>();
        if (membresias.isEmpty()) return resultado;
        List<Long> ids = membresias.stream().map(Membresia::getId).toList();
        em.createQuery("""
                        SELECT DISTINCT c FROM Cuota c LEFT JOIN FETCH c.descuentos
                        WHERE c.membresia.id IN :ids AND c.status = 0 ORDER BY c.id""", Cuota.class)
                .setParameter("ids", ids).getResultList()
                // Si hubiera más de una (no debería), manda la última no anulada.
                .forEach(c -> {
                    Cuota previa = resultado.get(c.getMembresia().getId());
                    if (previa == null || c.getEstado() != CuotaEstadoEnum.ANULADA) {
                        resultado.put(c.getMembresia().getId(), c);
                    }
                });
        return resultado;
    }

    public Cuota cuota(Membresia membresia) {
        return cuotas(List.of(membresia)).get(membresia.getId());
    }

    public static CuotaEstadoEnum estadoCobro(Cuota cuota) {
        return cuota == null ? null : cuota.getEstado();
    }

    /** Membresías que ya tienen una sucesora (renovación o cambio de plan) no anulada. */
    public Set<Long> conSucesora(Collection<Membresia> membresias) {
        if (membresias.isEmpty()) return Set.of();
        return new HashSet<>(em.createQuery("""
                        SELECT mb.membresiaAnterior.id FROM Membresia mb
                        WHERE mb.membresiaAnterior.id IN :ids AND mb.status = 0
                          AND mb.estado <> com.blue.open.Code.Entity.Enums.MembresiaEstadoEnum.BAJA""", Long.class)
                .setParameter("ids", membresias.stream().map(Membresia::getId).toList()).getResultList());
    }

    // ---------------------------------------------------------------- Configuración del club

    public MembresiaDTO.Config config(Company company) {
        Configuration configuracion = configurationRepository.findFirstByNameAndCompany(CONFIG, company);
        if (configuracion == null || configuracion.getValue() == null || configuracion.getValue().isBlank()) {
            return new MembresiaDTO.Config();
        }
        try {
            return objectMapper.readValue(configuracion.getValue(), MembresiaDTO.Config.class);
        } catch (JsonProcessingException e) {
            return new MembresiaDTO.Config();
        }
    }

    public MembresiaDTO.Config guardarConfig(Company company, MembresiaDTO.Config valor) {
        Configuration configuracion = configurationRepository.findFirstByNameAndCompany(CONFIG, company);
        if (configuracion == null) {
            configuracion = new Configuration();
            configuracion.setName(CONFIG);
            configuracion.setCompany(company);
            configuracion.setGlobal(false);
        }
        try {
            configuracion.setValue(objectMapper.writeValueAsString(valor));
        } catch (JsonProcessingException e) {
            throw new CustomException("No se pudo guardar la configuración");
        }
        configurationRepository.save(configuracion);
        return valor;
    }

    // ---------------------------------------------------------------- Armado de respuestas

    public MembresiaDTO.Pase pase(Pase p, long vigentes, LocalDate hoy) {
        MembresiaDTO.Pase dto = new MembresiaDTO.Pase();
        dto.setUuid(p.getUuid());
        dto.setNombre(p.getNombre());
        dto.setDescripcion(p.getDescripcion());
        dto.setCategoria(p.getCategoria());
        dto.setModalidad(p.getModalidad());
        dto.setDuracionDias(p.getDuracionDias());
        dto.setPrecio(p.getPrecio());
        dto.setCantidadIngresos(p.getCantidadIngresos());
        dto.setDiasPermitidos(p.getDiasPermitidos() == null ? List.of() : new ArrayList<DiaSemanaEnum>(p.dias()));
        dto.setHoraDesde(p.getHoraDesde());
        dto.setHoraHasta(p.getHoraHasta());
        dto.setRenovacionAutomatica(p.isRenovacionAutomatica());
        dto.setCongelamientoExtiende(p.isCongelamientoExtiende());
        dto.setIncluyeCuotaActividades(p.isIncluyeCuotaActividades());
        dto.setFamiliar(p.isFamiliar());
        dto.setMaxIntegrantes(p.getMaxIntegrantes());
        dto.setVentaPortal(p.isVentaPortal());
        dto.setVentaDesde(p.getVentaDesde());
        dto.setVentaHasta(p.getVentaHasta());
        dto.setRestricciones(p.getRestricciones());
        dto.setActivo(p.isActivo());
        dto.setDisponible(p.seVendeEl(hoy));
        p.getActividades().stream().filter(a -> a.getStatus() == 0)
                .sorted(Comparator.comparing(a -> a.getNombre().toLowerCase()))
                .forEach(a -> dto.getActividades().add(new MembresiaDTO.Referencia(a.getUuid(), a.getNombre())));
        p.getAmenities().stream().filter(a -> a.getStatus() == 0)
                .sorted(Comparator.comparing(a -> a.getNombre().toLowerCase()))
                .forEach(a -> dto.getAmenities().add(new MembresiaDTO.Referencia(a.getUuid(), a.getNombre())));
        p.getDescuentos().stream().filter(d -> d.getStatus() == 0)
                .forEach(d -> dto.getDescuentos().add(new MembresiaDTO.Referencia(d.getUuid(), d.getTitulo())));
        dto.setMembresiasVigentes(vigentes);
        return dto;
    }

    public static String nombre(Member m) {
        return (m.getUser().getNombre() + " " + m.getUser().getApellido()).trim();
    }

    public MembresiaDTO.Membresia membresia(Membresia m, Cuota cuota, boolean renovada, LocalDate hoy,
                                            boolean gestiona, boolean conAccesos) {
        MembresiaDTO.Membresia dto = new MembresiaDTO.Membresia();
        dto.setUuid(m.getUuid());
        dto.setPase(pase(m.getPase(), 0, hoy));
        dto.setSocio(new MembresiaDTO.Referencia(m.getMember().getUuid(), nombre(m.getMember())));
        dto.setNumeroSocio(m.getMember().getNumeroSocio());
        m.getIntegrantes().forEach(i -> dto.getIntegrantes().add(new MembresiaDTO.Referencia(i.getUuid(), nombre(i))));
        dto.setOrigen(m.getOrigen());
        dto.setEstado(m.getEstado());
        MembresiaSituacionEnum situacion = MembresiaReglas.situacion(m, estadoCobro(cuota), hoy);
        dto.setSituacion(situacion);
        dto.setFechaInicio(m.getFechaInicio());
        dto.setFechaFin(m.getFechaFin());
        dto.setFechaFinOriginal(m.getFechaFinOriginal());
        dto.setIngresosPermitidos(m.getIngresosPermitidos());
        dto.setIngresosUsados(m.getIngresosUsados());
        dto.setIngresosDisponibles(m.ingresosDisponibles());
        dto.setRenovacionAutomatica(m.isRenovacionAutomatica());
        dto.setPrecio(m.getPrecio());
        dto.setFechaBaja(m.getFechaBaja());
        dto.setMotivoBaja(m.getMotivoBaja());
        dto.setMembresiaAnteriorUuid(m.getMembresiaAnterior() == null ? null : m.getMembresiaAnterior().getUuid());
        dto.setRenovada(renovada);
        dto.setCobro(cobro(cuota));
        m.congelamientosActivos().stream().sorted(Comparator.comparing(MembresiaCongelamiento::getFechaDesde).reversed())
                .forEach(c -> {
                    MembresiaDTO.Congelamiento cd = new MembresiaDTO.Congelamiento();
                    cd.setUuid(c.getUuid());
                    cd.setDesde(c.getFechaDesde());
                    cd.setHasta(c.getFechaHasta());
                    cd.setDias(c.getDias());
                    cd.setMotivo(c.getMotivo());
                    cd.setExtiendeVencimiento(c.isExtiendeVencimiento());
                    cd.setAutorizadoPor(c.getAutorizadoPor() == null ? null
                            : (c.getAutorizadoPor().getNombre() + " " + c.getAutorizadoPor().getApellido()).trim());
                    cd.setRegistrado(c.getDateCreated() == null ? null : c.getDateCreated().toLocalDateTime());
                    dto.getCongelamientos().add(cd);
                });
        if (conAccesos) {
            em.createQuery("""
                            SELECT a FROM MembresiaAcceso a JOIN FETCH a.member mm JOIN FETCH mm.user
                            WHERE a.membresia.id = :id AND a.status = 0 ORDER BY a.fecha DESC""", MembresiaAcceso.class)
                    .setParameter("id", m.getId()).setMaxResults(10).getResultList()
                    .forEach(a -> {
                        MembresiaDTO.Acceso ad = new MembresiaDTO.Acceso();
                        ad.setFecha(a.getFecha());
                        ad.setPermitido(a.isPermitido());
                        ad.setMotivo(a.getMotivo());
                        ad.setSocio(nombre(a.getMember()));
                        dto.getUltimosAccesos().add(ad);
                    });
        }
        boolean activa = m.getEstado() == MembresiaEstadoEnum.ACTIVA;
        boolean noTermino = !hoy.isAfter(m.getFechaFin());
        dto.setPuedeRenovar(gestiona && activa && !renovada && m.getPase().isActivo());
        dto.setPuedeCongelar(gestiona && activa && noTermino && situacion != MembresiaSituacionEnum.PENDIENTE_PAGO);
        dto.setPuedeCambiarPlan(gestiona && activa && noTermino && !renovada);
        dto.setPuedeDarDeBaja(gestiona && activa && noTermino);
        return dto;
    }

    public MembresiaDTO.Cobro cobro(Cuota cuota) {
        if (cuota == null) return null;
        MembresiaDTO.Cobro dto = new MembresiaDTO.Cobro();
        dto.setCuotaUuid(cuota.getUuid());
        dto.setEstado(cuota.getEstado());
        dto.setMonto(cuota.getMonto());
        dto.setMontoFinal(cuota.montoFinal());
        dto.setVencimiento(cuota.getVencimiento());
        if (cuota.getEstado() == CuotaEstadoEnum.PAGADA) {
            em.createQuery("""
                            SELECT mv FROM Movimiento mv WHERE mv.cuota.id = :id AND mv.status = 0
                              AND mv.estado = :activo AND mv.tipo = :ingreso ORDER BY mv.id DESC""", Movimiento.class)
                    .setParameter("id", cuota.getId()).setParameter("activo", MovimientoEstadoEnum.ACTIVO)
                    .setParameter("ingreso", MovimientoTipoEnum.INGRESO).setMaxResults(1).getResultList()
                    .forEach(mv -> {
                        dto.setFechaPago(mv.getFecha());
                        dto.setMedioPago(mv.getMedioPago() == null ? null : mv.getMedioPago().name());
                    });
        }
        return dto;
    }

    public long vigentesPorPase(Long paseId, LocalDate hoy) {
        List<Membresia> lista = em.createQuery("""
                        SELECT mb FROM Membresia mb WHERE mb.pase.id = :paseId AND mb.status = 0
                          AND mb.estado = com.blue.open.Code.Entity.Enums.MembresiaEstadoEnum.ACTIVA
                          AND mb.fechaFin >= :hoy""", Membresia.class)
                .setParameter("paseId", paseId).setParameter("hoy", hoy).getResultList();
        Map<Long, Cuota> cuotas = cuotas(lista);
        return lista.stream().filter(m -> MembresiaReglas.situacion(m, estadoCobro(cuotas.get(m.getId())), hoy)
                == MembresiaSituacionEnum.VIGENTE).count();
    }

    public EntityManager em() {
        return em;
    }
}
