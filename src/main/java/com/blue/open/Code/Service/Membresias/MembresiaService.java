package com.blue.open.Code.Service.Membresias;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.MembresiaDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Cuota;
import com.blue.open.Code.Entity.Db.Descuento;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.Membresia;
import com.blue.open.Code.Entity.Db.MembresiaCongelamiento;
import com.blue.open.Code.Entity.Db.Pase;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.MemberEstadoEnum;
import com.blue.open.Code.Entity.Enums.MembresiaEstadoEnum;
import com.blue.open.Code.Entity.Enums.MembresiaOrigenEnum;
import com.blue.open.Code.Entity.Enums.MembresiaSituacionEnum;
import com.blue.open.Code.Repository.CuotaRepository;
import com.blue.open.Code.Service.MemberAuthService;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Membresías: contratar, renovar, cambiar de plan, congelar y dar de baja. Cada venta genera una cuota de
 * tipo PASE que se cobra por tesorería o Mercado Pago como cualquier otra; el pase habilita cuando está paga.
 * Nada se pisa: renovaciones y cambios crean una membresía nueva enlazada a la anterior.
 */
@Service
public class MembresiaService {

    /** Margen para registrar un pase que en la práctica arrancó unos días antes. */
    private static final int DIAS_RETROACTIVOS = 31;
    private static final int MAX_DIAS_CONGELAMIENTO = 365;

    @Autowired
    private AuthService authService;

    @Autowired
    private MemberAuthService memberAuthService;

    @Autowired
    private CuotaRepository cuotaRepository;

    @Autowired
    private MembresiaSoporte soporte;

    // ================================================================ Legajo del socio (admin / recepción)

    @Transactional(readOnly = true)
    public MembresiaDTO.Legajo legajo(String memberUuid) {
        authService.valid(List.of(PermissionEnum.MEMBERSHIPS_MANAGE));
        Company company = authService.getCurrentCompanyOrThrow();
        Member socio = soporte.socio(memberUuid, company.getId());
        MembresiaDTO.Legajo legajo = new MembresiaDTO.Legajo();
        legajo.setSocio(new MembresiaDTO.Referencia(socio.getUuid(), MembresiaSoporte.nombre(socio)));
        legajo.setTitular(socio.isTitular());
        soporte.grupo(socio).forEach(m -> legajo.getGrupo().add(new MembresiaDTO.Referencia(m.getUuid(), MembresiaSoporte.nombre(m))));
        legajo.setMembresias(armar(soporte.deSocio(socio.getId(), company.getId()), true));
        legajo.setVigente(legajo.getMembresias().stream()
                .filter(m -> m.getSituacion() == MembresiaSituacionEnum.VIGENTE).findFirst().orElse(null));
        legajo.setExigirPaseParaIngresar(soporte.config(company).isExigirPaseParaIngresar());
        return legajo;
    }

    @Transactional
    public MembresiaDTO.Membresia contratar(MembresiaDTO.ContratarCommand c) {
        User usuario = authService.valid(List.of(PermissionEnum.MEMBERSHIPS_MANAGE));
        Company company = authService.getCurrentCompanyOrThrow();
        Member socio = soporte.socio(c.getMemberUuid(), company.getId());
        Pase pase = soporte.porUuid(Pase.class, c.getPaseUuid(), company.getId(), "Pase");
        exigirEnVenta(pase);
        List<Member> integrantes = soporte.porUuids(Member.class, c.getIntegrantesUuids(), company.getId(), "Integrante");
        LocalDate inicio = c.getFechaInicio() == null ? soporte.hoy() : c.getFechaInicio();
        Membresia nueva = crear(company, pase, socio, inicio, integrantes, descuentos(pase, c.getDescuentosUuids(), company),
                MembresiaOrigenEnum.ALTA, null, usuario);
        return armarUna(nueva, true);
    }

    @Transactional
    public MembresiaDTO.Membresia renovar(String uuid, MembresiaDTO.RenovarCommand c) {
        User usuario = authService.valid(List.of(PermissionEnum.MEMBERSHIPS_MANAGE));
        Company company = authService.getCurrentCompanyOrThrow();
        Membresia anterior = soporte.membresia(uuid, company.getId());
        return armarUna(renovarInterno(company, anterior,
                descuentos(anterior.getPase(), c == null ? List.of() : c.getDescuentosUuids(), company), usuario), true);
    }

    /** También lo usa la renovación automática (usuario null). */
    Membresia renovarInterno(Company company, Membresia anterior, List<Descuento> descuentos, User usuario) {
        exigirActiva(anterior);
        if (!soporte.conSucesora(List.of(anterior)).isEmpty()) {
            throw conflicto("Esta membresía ya fue renovada");
        }
        Pase pase = anterior.getPase();
        if (!pase.isActivo()) {
            throw conflicto("El pase " + pase.getNombre() + " ya no se vende: elegí otro plan");
        }
        LocalDate hoy = soporte.hoy();
        LocalDate inicio = anterior.getFechaFin().isBefore(hoy) ? hoy : anterior.getFechaFin().plusDays(1);
        return crear(company, pase, anterior.getMember(), inicio, new ArrayList<>(anterior.getIntegrantes()), descuentos,
                MembresiaOrigenEnum.RENOVACION, anterior, usuario);
    }

    @Transactional
    public MembresiaDTO.Membresia cambiarPlan(String uuid, MembresiaDTO.CambiarPlanCommand c) {
        User usuario = authService.valid(List.of(PermissionEnum.MEMBERSHIPS_MANAGE));
        Company company = authService.getCurrentCompanyOrThrow();
        Membresia anterior = soporte.membresia(uuid, company.getId());
        exigirActiva(anterior);
        LocalDate hoy = soporte.hoy();
        if (hoy.isAfter(anterior.getFechaFin())) {
            throw conflicto("La membresía ya venció: contratá un pase nuevo");
        }
        if (!soporte.conSucesora(List.of(anterior)).isEmpty()) {
            throw conflicto("Esta membresía ya tiene una renovación cargada: dala de baja primero");
        }
        Pase pase = soporte.porUuid(Pase.class, c.getPaseUuid(), company.getId(), "Pase");
        exigirEnVenta(pase);
        List<Member> integrantes = soporte.porUuids(Member.class, c.getIntegrantesUuids(), company.getId(), "Integrante");
        LocalDate inicio = c.getFechaInicio() == null ? hoy : c.getFechaInicio();

        cerrar(anterior, MembresiaEstadoEnum.REEMPLAZADA, "Cambio al plan " + pase.getNombre(), usuario);
        Membresia nueva = crear(company, pase, anterior.getMember(), inicio, integrantes,
                descuentos(pase, c.getDescuentosUuids(), company), MembresiaOrigenEnum.CAMBIO_PLAN, anterior, usuario);
        return armarUna(nueva, true);
    }

    @Transactional
    public MembresiaDTO.Membresia congelar(String uuid, MembresiaDTO.CongelarCommand c) {
        User usuario = authService.valid(List.of(PermissionEnum.MEMBERSHIPS_MANAGE));
        Company company = authService.getCurrentCompanyOrThrow();
        Membresia m = soporte.membresia(uuid, company.getId());
        exigirActiva(m);
        if (MembresiaReglas.situacion(m, MembresiaSoporte.estadoCobro(soporte.cuota(m)), soporte.hoy())
                == MembresiaSituacionEnum.PENDIENTE_PAGO) {
            throw conflicto("No se puede congelar un pase que todavía no está pago");
        }
        if (c.getHasta().isBefore(c.getDesde())) {
            throw invalido("La fecha hasta no puede ser anterior a la fecha desde");
        }
        if (c.getDesde().isBefore(m.getFechaInicio()) || c.getDesde().isAfter(m.getFechaFin())) {
            throw invalido("El congelamiento tiene que empezar dentro de la vigencia del pase");
        }
        int dias = MembresiaReglas.diasEntre(c.getDesde(), c.getHasta());
        if (dias > MAX_DIAS_CONGELAMIENTO) {
            throw invalido("Un congelamiento no puede superar " + MAX_DIAS_CONGELAMIENTO + " días");
        }
        if (MembresiaReglas.seSuperpone(m, c.getDesde(), c.getHasta())) {
            throw conflicto("Ya hay un congelamiento cargado en esas fechas");
        }
        MembresiaCongelamiento cong = new MembresiaCongelamiento();
        cong.setMembresia(m);
        cong.setFechaDesde(c.getDesde());
        cong.setFechaHasta(c.getHasta());
        cong.setDias(dias);
        cong.setMotivo(c.getMotivo().trim());
        cong.setExtiendeVencimiento(m.getPase().isCongelamientoExtiende());
        cong.setAutorizadoPor(usuario);
        soporte.em().persist(cong);
        m.getCongelamientos().add(cong);
        if (cong.isExtiendeVencimiento()) {
            m.setFechaFin(m.getFechaFin().plusDays(dias));
        }
        return armarUna(m, true);
    }

    @Transactional
    public MembresiaDTO.Membresia anularCongelamiento(String congelamientoUuid) {
        authService.valid(List.of(PermissionEnum.MEMBERSHIPS_MANAGE));
        Company company = authService.getCurrentCompanyOrThrow();
        List<MembresiaCongelamiento> lista = soporte.em().createQuery("""
                        SELECT c FROM MembresiaCongelamiento c JOIN FETCH c.membresia m
                        WHERE c.uuid = :uuid AND c.status = 0 AND m.company.id = :companyId""", MembresiaCongelamiento.class)
                .setParameter("uuid", congelamientoUuid).setParameter("companyId", company.getId()).getResultList();
        if (lista.isEmpty()) {
            throw new CustomException(HttpStatus.NOT_FOUND, "Congelamiento no encontrado",
                    "MembresiaService | congelamientoUuid: " + congelamientoUuid);
        }
        MembresiaCongelamiento cong = lista.get(0);
        Membresia m = cong.getMembresia();
        exigirActiva(m);
        cong.setStatus(1);
        if (cong.isExtiendeVencimiento()) {
            m.setFechaFin(m.getFechaFin().minusDays(cong.getDias()));
        }
        return armarUna(m, true);
    }

    @Transactional
    public MembresiaDTO.Membresia darDeBaja(String uuid, MembresiaDTO.BajaCommand c) {
        User usuario = authService.valid(List.of(PermissionEnum.MEMBERSHIPS_MANAGE));
        Company company = authService.getCurrentCompanyOrThrow();
        Membresia m = soporte.membresia(uuid, company.getId());
        exigirActiva(m);
        cerrar(m, MembresiaEstadoEnum.BAJA, c.getMotivo().trim(), usuario);
        return armarUna(m, true);
    }

    // ================================================================ Portal del socio

    @Transactional(readOnly = true)
    public MembresiaDTO.Portal portal() {
        authService.valid(List.of(PermissionEnum.PORTAL_MEMBERSHIPS_VIEW));
        Company company = authService.getCurrentCompanyOrThrow();
        Member actual = memberAuthService.requirePortalEligible(memberAuthService.getCurrentMemberOrThrow(company.getId()));
        MembresiaDTO.Portal portal = new MembresiaDTO.Portal();
        List<Member> alcance = actual.isTitular() ? soporte.grupo(actual) : List.of(actual);
        alcance.forEach(m -> portal.getGrupo().add(new MembresiaDTO.Referencia(m.getUuid(), MembresiaSoporte.nombre(m))));
        Map<Long, Membresia> unicas = new java.util.LinkedHashMap<>();
        alcance.forEach(m -> soporte.deSocio(m.getId(), company.getId()).forEach(mb -> unicas.putIfAbsent(mb.getId(), mb)));
        boolean compra = actual.isTitular() && authService.validBoolean(List.of(PermissionEnum.PORTAL_MEMBERSHIPS_BUY));
        List<MembresiaDTO.Membresia> dtos = armar(new ArrayList<>(unicas.values()), false);
        dtos.forEach(d -> {
            // En el portal solo se puede renovar (el resto lo hace el club) y si el pase se vende online.
            d.setPuedeRenovar(compra && d.getEstado() == MembresiaEstadoEnum.ACTIVA && !d.isRenovada()
                    && d.getPase().isActivo() && d.getPase().isVentaPortal());
            d.setPuedeCambiarPlan(false);
            d.setPuedeCongelar(false);
            d.setPuedeDarDeBaja(false);
        });
        dtos.sort((a, b) -> b.getFechaInicio().compareTo(a.getFechaInicio()));
        portal.setMembresias(dtos);
        portal.setPuedeContratar(compra);
        LocalDate hoy = soporte.hoy();
        if (compra) {
            new PaseConsulta(soporte).enVentaPortal(company.getId(), hoy)
                    .forEach(p -> portal.getPasesDisponibles().add(soporte.pase(p, 0, hoy)));
        }
        return portal;
    }

    @Transactional
    public MembresiaDTO.Membresia portalContratar(MembresiaDTO.PortalContratarCommand c) {
        User usuario = authService.valid(List.of(PermissionEnum.PORTAL_MEMBERSHIPS_BUY));
        Company company = authService.getCurrentCompanyOrThrow();
        Member titular = memberAuthService.requireTitular(
                memberAuthService.requirePortalEligible(memberAuthService.getCurrentMemberOrThrow(company.getId())));
        Pase pase = soporte.porUuid(Pase.class, c.getPaseUuid(), company.getId(), "Pase");
        if (!pase.isVentaPortal() || !pase.seVendeEl(soporte.hoy())) {
            throw conflicto("Este pase no se puede contratar desde el portal");
        }
        Member socio = c.getMemberUuid() == null || c.getMemberUuid().isBlank() ? titular
                : soporte.socio(c.getMemberUuid(), company.getId());
        Set<Long> grupo = soporte.grupo(titular).stream().map(Member::getId).collect(Collectors.toSet());
        if (!grupo.contains(socio.getId())) {
            throw new CustomException(HttpStatus.FORBIDDEN, "Solo podés contratar pases para tu grupo familiar",
                    "MembresiaService | portalContratar fuera del grupo");
        }
        List<Member> integrantes = soporte.porUuids(Member.class, c.getIntegrantesUuids(), company.getId(), "Integrante");
        LocalDate inicio = c.getFechaInicio() == null ? soporte.hoy() : c.getFechaInicio();
        if (inicio.isBefore(soporte.hoy())) {
            throw invalido("La fecha de inicio no puede ser anterior a hoy");
        }
        return armarUna(crear(company, pase, socio, inicio, integrantes, List.of(), MembresiaOrigenEnum.ALTA, null, usuario), false);
    }

    @Transactional
    public MembresiaDTO.Membresia portalRenovar(String uuid) {
        User usuario = authService.valid(List.of(PermissionEnum.PORTAL_MEMBERSHIPS_BUY));
        Company company = authService.getCurrentCompanyOrThrow();
        Member titular = memberAuthService.requireTitular(
                memberAuthService.requirePortalEligible(memberAuthService.getCurrentMemberOrThrow(company.getId())));
        Membresia anterior = soporte.membresia(uuid, company.getId());
        Set<Long> grupo = soporte.grupo(titular).stream().map(Member::getId).collect(Collectors.toSet());
        if (!grupo.contains(anterior.getMember().getId())) {
            throw new CustomException(HttpStatus.FORBIDDEN, "La membresía no es de tu grupo familiar",
                    "MembresiaService | portalRenovar fuera del grupo");
        }
        if (!anterior.getPase().isVentaPortal()) {
            throw conflicto("Este pase se renueva en el club");
        }
        return armarUna(renovarInterno(company, anterior, List.of(), usuario), false);
    }

    // ================================================================ Núcleo

    /** Crea la membresía y su cuota. Valida grupo familiar, superposición y descuentos. */
    Membresia crear(Company company, Pase pase, Member socio, LocalDate inicio, List<Member> integrantes,
                    List<Descuento> descuentos, MembresiaOrigenEnum origen, Membresia anterior, User usuario) {
        LocalDate hoy = soporte.hoy();
        if (socio.getEstado() == MemberEstadoEnum.INACTIVO) {
            throw conflicto("El socio está dado de baja");
        }
        if (origen == MembresiaOrigenEnum.ALTA || origen == MembresiaOrigenEnum.CAMBIO_PLAN) {
            if (inicio.isBefore(hoy.minusDays(DIAS_RETROACTIVOS))) {
                throw invalido("La fecha de inicio no puede ser anterior a " + DIAS_RETROACTIVOS + " días");
            }
        }
        if (inicio.isAfter(hoy.plusYears(1))) {
            throw invalido("La fecha de inicio no puede ser dentro de más de un año");
        }

        List<Member> cubiertos = validarIntegrantes(pase, socio, integrantes);
        LocalDate fin;
        try {
            fin = pase.getModalidad().fin(inicio, pase.getDuracionDias());
        } catch (IllegalArgumentException e) {
            throw invalido("El pase no tiene una duración válida");
        }
        validarSuperposicion(company, pase, cubiertos, inicio, fin, anterior);

        Membresia m = new Membresia();
        m.setCompany(company);
        m.setPase(pase);
        m.setMember(socio);
        m.setMembresiaAnterior(anterior);
        m.setOrigen(origen);
        m.setEstado(MembresiaEstadoEnum.ACTIVA);
        m.setFechaInicio(inicio);
        m.setFechaFin(fin);
        m.setFechaFinOriginal(fin);
        m.setIngresosPermitidos(pase.getCantidadIngresos());
        m.setIngresosUsados(0);
        m.setRenovacionAutomatica(pase.isRenovacionAutomatica());
        m.setPrecio(pase.getPrecio());
        m.setIntegrantes(new HashSet<>(integrantes));
        soporte.em().persist(m);

        Cuota cuota = Cuota.dePase(company, m, hoy, descuentos);
        cuotaRepository.save(cuota);
        return m;
    }

    /** Devuelve todos los cubiertos (socio + integrantes). */
    private List<Member> validarIntegrantes(Pase pase, Member socio, List<Member> integrantes) {
        List<Member> cubiertos = new ArrayList<>();
        cubiertos.add(socio);
        if (!pase.isFamiliar()) {
            if (!integrantes.isEmpty()) {
                throw invalido("El pase " + pase.getNombre() + " es individual: no lleva integrantes");
            }
            return cubiertos;
        }
        if (!socio.isTitular()) {
            throw invalido("Un pase familiar se contrata a nombre del titular del grupo");
        }
        Set<Long> grupo = soporte.grupo(socio).stream().map(Member::getId).collect(Collectors.toSet());
        for (Member integrante : integrantes) {
            if (integrante.getId().equals(socio.getId())) continue;
            if (!grupo.contains(integrante.getId())) {
                throw invalido(MembresiaSoporte.nombre(integrante) + " no pertenece al grupo familiar de "
                        + MembresiaSoporte.nombre(socio));
            }
            cubiertos.add(integrante);
        }
        integrantes.removeIf(i -> i.getId().equals(socio.getId()));
        if (cubiertos.size() < 2) {
            throw invalido("Elegí al menos un integrante del grupo para el pase familiar");
        }
        if (pase.getMaxIntegrantes() != null && cubiertos.size() > pase.getMaxIntegrantes()) {
            throw invalido("El pase " + pase.getNombre() + " cubre hasta " + pase.getMaxIntegrantes() + " personas");
        }
        return cubiertos;
    }

    /** No se puede tener dos veces el mismo pase en fechas que se pisan (para eso está renovar). */
    private void validarSuperposicion(Company company, Pase pase, List<Member> cubiertos, LocalDate inicio,
                                      LocalDate fin, Membresia anterior) {
        for (Member persona : cubiertos) {
            for (Membresia otra : soporte.deSocio(persona.getId(), company.getId())) {
                if (otra.getEstado() != MembresiaEstadoEnum.ACTIVA) continue;
                if (anterior != null && otra.getId().equals(anterior.getId())) continue;
                if (!otra.getPase().getId().equals(pase.getId())) continue;
                boolean seSuperpone = !inicio.isAfter(otra.getFechaFin()) && !fin.isBefore(otra.getFechaInicio());
                if (seSuperpone) {
                    throw conflicto(MembresiaSoporte.nombre(persona) + " ya tiene el pase " + pase.getNombre()
                            + " hasta el " + String.format("%02d/%02d/%d", otra.getFechaFin().getDayOfMonth(),
                            otra.getFechaFin().getMonthValue(), otra.getFechaFin().getYear()) + ": usá Renovar");
                }
            }
        }
    }

    private List<Descuento> descuentos(Pase pase, Collection<String> uuids, Company company) {
        List<Descuento> elegidos = soporte.porUuids(Descuento.class, uuids, company.getId(), "Descuento");
        Set<Long> permitidos = pase.getDescuentos().stream().map(Descuento::getId).collect(Collectors.toSet());
        for (Descuento d : elegidos) {
            if (!permitidos.contains(d.getId())) {
                throw invalido("El descuento " + d.getTitulo() + " no se puede aplicar al pase " + pase.getNombre());
            }
        }
        return elegidos;
    }

    /** Baja o reemplazo: si la cuota no se pagó, se anula (no queda deuda por un pase que no se usa). */
    private void cerrar(Membresia m, MembresiaEstadoEnum estado, String motivo, User usuario) {
        m.setEstado(estado);
        m.setFechaBaja(soporte.hoy());
        m.setMotivoBaja(motivo.length() > 255 ? motivo.substring(0, 255) : motivo);
        Cuota cuota = soporte.cuota(m);
        if (cuota != null && cuota.estaImpaga()) {
            cuota.anular(usuario, (estado == MembresiaEstadoEnum.BAJA ? "Baja del pase: " : "") + m.getMotivoBaja());
        }
    }

    private void exigirEnVenta(Pase pase) {
        if (!pase.seVendeEl(soporte.hoy())) {
            throw conflicto("El pase " + pase.getNombre() + " no está a la venta");
        }
    }

    private static void exigirActiva(Membresia m) {
        if (m.getEstado() != MembresiaEstadoEnum.ACTIVA) {
            throw conflicto("La membresía ya fue " + (m.getEstado() == MembresiaEstadoEnum.BAJA ? "dada de baja" : "reemplazada"));
        }
    }

    private List<MembresiaDTO.Membresia> armar(List<Membresia> lista, boolean gestiona) {
        Map<Long, Cuota> cuotas = soporte.cuotas(lista);
        Set<Long> sucesoras = soporte.conSucesora(lista);
        LocalDate hoy = soporte.hoy();
        List<MembresiaDTO.Membresia> dtos = new ArrayList<>();
        for (Membresia m : lista) {
            dtos.add(soporte.membresia(m, cuotas.get(m.getId()), sucesoras.contains(m.getId()), hoy, gestiona, true));
        }
        return dtos;
    }

    private MembresiaDTO.Membresia armarUna(Membresia m, boolean gestiona) {
        soporte.em().flush();
        return armar(List.of(m), gestiona).get(0);
    }

    private static CustomException conflicto(String mensaje) {
        return new CustomException(HttpStatus.CONFLICT, mensaje, "MembresiaService | " + mensaje);
    }

    private static CustomException invalido(String mensaje) {
        return new CustomException(HttpStatus.BAD_REQUEST, mensaje, "MembresiaService | " + mensaje);
    }

    /** Consulta chica para no depender de PaseService (que exige permisos de admin). */
    record PaseConsulta(MembresiaSoporte soporte) {
        List<Pase> enVentaPortal(Long companyId, LocalDate hoy) {
            return soporte.em().createQuery("""
                            SELECT DISTINCT p FROM Pase p LEFT JOIN FETCH p.actividades LEFT JOIN FETCH p.amenities
                            WHERE p.company.id = :companyId AND p.status = 0 AND p.activo = true AND p.ventaPortal = true
                            ORDER BY p.precio""", Pase.class)
                    .setParameter("companyId", companyId).getResultList().stream()
                    .filter(p -> p.seVendeEl(hoy)).filter(Objects::nonNull).toList();
        }
    }
}
