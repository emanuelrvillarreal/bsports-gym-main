package com.blue.open.Code.Service.Membresias;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.MembresiaDTO;
import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.Amenity;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Descuento;
import com.blue.open.Code.Entity.Db.Pase;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/**
 * Oferta de pases de cada club. Solo ADMIN configura (PASSES_MANAGE); recepción consulta los disponibles para
 * vender (MEMBERSHIPS_MANAGE). Un pase con membresías no se borra: se desactiva y deja de venderse.
 */
@Service
public class PaseService {

    @Autowired
    private AuthService authService;

    @Autowired
    private MembresiaSoporte soporte;

    @Transactional(readOnly = true)
    public List<MembresiaDTO.Pase> listar() {
        authService.valid(List.of(PermissionEnum.PASSES_MANAGE));
        Company company = authService.getCurrentCompanyOrThrow();
        LocalDate hoy = soporte.hoy();
        return pases(company.getId(), false).stream()
                .map(p -> soporte.pase(p, soporte.vigentesPorPase(p.getId(), hoy), hoy)).toList();
    }

    /** Pases que hoy se pueden vender (para el alta desde el legajo del socio). */
    @Transactional(readOnly = true)
    public List<MembresiaDTO.Pase> disponibles() {
        authService.valid(List.of(PermissionEnum.MEMBERSHIPS_MANAGE));
        Company company = authService.getCurrentCompanyOrThrow();
        LocalDate hoy = soporte.hoy();
        return pases(company.getId(), true).stream().filter(p -> p.seVendeEl(hoy))
                .map(p -> soporte.pase(p, 0, hoy)).toList();
    }

    List<Pase> pases(Long companyId, boolean soloActivos) {
        return soporte.em().createQuery("""
                        SELECT DISTINCT p FROM Pase p LEFT JOIN FETCH p.actividades LEFT JOIN FETCH p.amenities
                        WHERE p.company.id = :companyId AND p.status = 0""" + (soloActivos ? " AND p.activo = true" : ""), Pase.class)
                .setParameter("companyId", companyId).getResultList().stream()
                .sorted(Comparator.comparing(Pase::isActivo).reversed()
                        .thenComparing(p -> p.getNombre().toLowerCase()))
                .toList();
    }

    @Transactional
    public MembresiaDTO.Pase crear(MembresiaDTO.PaseCommand command) {
        authService.valid(List.of(PermissionEnum.PASSES_MANAGE));
        Company company = authService.getCurrentCompanyOrThrow();
        Pase pase = new Pase();
        pase.setCompany(company);
        aplicar(pase, command, company);
        soporte.em().persist(pase);
        return soporte.pase(pase, 0, soporte.hoy());
    }

    @Transactional
    public MembresiaDTO.Pase editar(String uuid, MembresiaDTO.PaseCommand command) {
        authService.valid(List.of(PermissionEnum.PASSES_MANAGE));
        Company company = authService.getCurrentCompanyOrThrow();
        Pase pase = soporte.porUuid(Pase.class, uuid, company.getId(), "Pase");
        aplicar(pase, command, company);
        LocalDate hoy = soporte.hoy();
        return soporte.pase(pase, soporte.vigentesPorPase(pase.getId(), hoy), hoy);
    }

    /** Solo si nunca se vendió; si no, hay que desactivarlo para conservar el historial. */
    @Transactional
    public void eliminar(String uuid) {
        authService.valid(List.of(PermissionEnum.PASSES_MANAGE));
        Company company = authService.getCurrentCompanyOrThrow();
        Pase pase = soporte.porUuid(Pase.class, uuid, company.getId(), "Pase");
        Long ventas = soporte.em().createQuery(
                        "SELECT COUNT(mb) FROM Membresia mb WHERE mb.pase.id = :id AND mb.status = 0", Long.class)
                .setParameter("id", pase.getId()).getSingleResult();
        if (ventas > 0) {
            throw new CustomException(HttpStatus.CONFLICT,
                    "El pase ya se vendió " + ventas + " vez/veces: desactivalo para que no se venda más",
                    "PaseService | eliminar con membresías | uuid: " + uuid);
        }
        pase.setStatus(1);
    }

    @Transactional(readOnly = true)
    public MembresiaDTO.Config config() {
        authService.valid(List.of(PermissionEnum.MEMBERSHIPS_MANAGE));
        return soporte.config(authService.getCurrentCompanyOrThrow());
    }

    @Transactional
    public MembresiaDTO.Config guardarConfig(MembresiaDTO.Config config) {
        authService.valid(List.of(PermissionEnum.PASSES_MANAGE));
        return soporte.guardarConfig(authService.getCurrentCompanyOrThrow(), config);
    }

    private void aplicar(Pase pase, MembresiaDTO.PaseCommand c, Company company) {
        validar(c);
        pase.setNombre(c.getNombre().trim());
        pase.setDescripcion(texto(c.getDescripcion()));
        pase.setCategoria(c.getCategoria());
        pase.setModalidad(c.getModalidad());
        pase.setDuracionDias(c.getModalidad().requiereDuracionDias() ? c.getDuracionDias() : null);
        pase.setPrecio(c.getPrecio().setScale(2, java.math.RoundingMode.HALF_UP));
        pase.setCantidadIngresos(c.getCantidadIngresos());
        pase.definirDias(c.getDiasPermitidos());
        pase.setHoraDesde(c.getHoraDesde());
        pase.setHoraHasta(c.getHoraHasta());
        pase.setRenovacionAutomatica(c.isRenovacionAutomatica());
        pase.setCongelamientoExtiende(c.isCongelamientoExtiende());
        pase.setIncluyeCuotaActividades(c.isIncluyeCuotaActividades());
        pase.setFamiliar(c.isFamiliar());
        pase.setMaxIntegrantes(c.isFamiliar() ? c.getMaxIntegrantes() : null);
        pase.setVentaPortal(c.isVentaPortal());
        pase.setVentaDesde(c.getVentaDesde());
        pase.setVentaHasta(c.getVentaHasta());
        pase.setRestricciones(texto(c.getRestricciones()));
        pase.setActivo(c.isActivo());
        Long companyId = company.getId();
        pase.setActividades(new HashSet<>(soporte.porUuids(Actividad.class, c.getActividadesUuids(), companyId, "Actividad")));
        pase.setAmenities(new HashSet<>(soporte.porUuids(Amenity.class, c.getAmenitiesUuids(), companyId, "Instalación")));
        pase.setDescuentos(new HashSet<>(soporte.porUuids(Descuento.class, c.getDescuentosUuids(), companyId, "Descuento")));
    }

    static void validar(MembresiaDTO.PaseCommand c) {
        if (c.getModalidad().requiereDuracionDias() && c.getDuracionDias() == null) {
            throw error("Indicá cuántos días dura el pase");
        }
        if (c.getModalidad() == com.blue.open.Code.Entity.Enums.PaseModalidadEnum.POR_INGRESOS
                && c.getCantidadIngresos() == null) {
            throw error("Un pase por cantidad de ingresos necesita la cantidad");
        }
        if ((c.getHoraDesde() == null) != (c.getHoraHasta() == null)) {
            throw error("Completá el horario desde y hasta, o dejá los dos vacíos");
        }
        if (c.getHoraDesde() != null && c.getHoraDesde().equals(c.getHoraHasta())) {
            throw error("El horario desde y hasta no pueden ser iguales");
        }
        if (c.isFamiliar() && c.getMaxIntegrantes() == null) {
            throw error("Indicá cuántas personas cubre el pase familiar");
        }
        if (c.getVentaDesde() != null && c.getVentaHasta() != null && c.getVentaHasta().isBefore(c.getVentaDesde())) {
            throw error("La fecha de fin de venta no puede ser anterior a la de inicio");
        }
        if (c.getPrecio() != null && c.getPrecio().compareTo(new BigDecimal("100000000")) >= 0) {
            throw error("Precio demasiado grande");
        }
    }

    private static CustomException error(String mensaje) {
        return new CustomException(HttpStatus.BAD_REQUEST, mensaje, "PaseService | " + mensaje);
    }

    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
