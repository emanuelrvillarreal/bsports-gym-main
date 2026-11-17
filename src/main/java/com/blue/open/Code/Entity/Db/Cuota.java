package com.blue.open.Code.Entity.Db;

import com.blue.open.Code.Entity.Enums.CuotaEstadoEnum;
import com.blue.open.Code.Entity.Enums.CuotaTipoEnum;
import com.blue.open.Core.Entity.Db.AbstractEntity;
import com.blue.open.Core.Exceptions.CustomException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Getter
@Setter(AccessLevel.NONE)
@Table(name = "cuotas")
@Entity
public class Cuota extends AbstractEntity {

    private static final DateTimeFormatter MES_ANIO_FORMAT =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es-AR"));

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "persona_id", nullable = false)
    private Member persona;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "titular_id", nullable = false)
    private Member titular;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "tipo", nullable = false, length = 20)
    private CuotaTipoEnum tipo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actividad_id")
    private Actividad actividad;

    /** Solo en cuotas de tipo PASE: la membresia que se cobra. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membresia_id")
    private Membresia membresia;

    @Column(name = "fecha_generacion", nullable = false)
    private LocalDate fechaGeneracion;

    /** Importe de lista, sin descuento aplicado. Para cobrar usar {@link #montoFinal()}. */
    @Column(name = "monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    /** Descuentos aplicados a esta cuota — snapshot inmutable, ver {@link CuotaDescuento}. */
    @OneToMany(mappedBy = "cuota", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CuotaDescuento> descuentos = new ArrayList<>();

    /** Recargo por pago fuera de término. Se aplica una sola vez ({@link #recargoAplicadoAt}); 0 si no corresponde. */
    @Column(name = "recargo", nullable = false, precision = 12, scale = 2)
    private BigDecimal recargo = BigDecimal.ZERO;

    @Column(name = "recargo_aplicado_at")
    private LocalDate recargoAplicadoAt;

    @Column(name = "vencimiento", nullable = false)
    private LocalDate vencimiento;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "estado", nullable = false, length = 20)
    private CuotaEstadoEnum estado;

    @Column(name = "concepto", nullable = false)
    private String concepto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anulada_por")
    private User anuladaPor;

    @Column(name = "anulada_motivo", length = 512)
    private String anuladaMotivo;

    @Column(name = "anulada_at")
    private Timestamp anuladaAt;

    protected Cuota() {
    }

    /**
     * Crea una cuota de actividad para el período de {@code fechaGeneracion}, dejando la
     * entidad con todos sus invariantes satisfechos.
     *
     * @param descuentos descuentos vigentes a aplicar; {@code null} o vacía equivale a sin descuento
     */
    public static Cuota deActividad(
            Company company,
            Member persona,
            Actividad actividad,
            LocalDate fechaGeneracion,
            List<Descuento> descuentos) {

        requireNotNull(company, "company");
        requireNotNull(persona, "persona");
        requireNotNull(actividad, "actividad");
        requireNotNull(fechaGeneracion, "fechaGeneracion");

        requireMismaCompany(company, persona.getCompany(), "El socio no pertenece al club");
        requireMismaCompany(company, actividad.getCompany(), "La actividad no pertenece al club");

        BigDecimal importe = actividad.getImporteCuota();
        if (importe == null || importe.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "La actividad no tiene importe de cuota configurado",
                    "Cuota | deActividad | actividadId: " + actividad.getId());
        }

        Cuota cuota = new Cuota();
        cuota.company = company;
        cuota.persona = persona;
        cuota.titular = resolveTitular(persona);
        cuota.tipo = CuotaTipoEnum.ACTIVIDAD;
        cuota.actividad = actividad;
        cuota.monto = importe;
        cuota.concepto = "Cuota actividad " + actividad.getNombre() + " " + formatMesAnio(fechaGeneracion);
        cuota.aplicarPeriodo(fechaGeneracion, company);
        cuota.aplicarDescuentos(descuentos);
        return cuota;
    }

    /**
     * Crea una cuota social para el período de {@code fechaGeneracion}, dejando la entidad
     * con todos sus invariantes satisfechos.
     *
     * @param descuentos descuentos vigentes a aplicar; {@code null} o vacía equivale a sin descuento
     */
    public static Cuota deSocial(
            Company company,
            Member persona,
            LocalDate fechaGeneracion,
            List<Descuento> descuentos) {

        requireNotNull(company, "company");
        requireNotNull(persona, "persona");
        requireNotNull(fechaGeneracion, "fechaGeneracion");

        requireMismaCompany(company, persona.getCompany(), "El socio no pertenece al club");

        if (!company.tieneCuotaSocialConfigurada()) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "El gimnasio no tiene cuota social habilitada",
                    "Cuota | deSocial | companyId: " + company.getId());
        }

        CuotaSocialRango rango = persona.getCuotaSocialRango();
        if (rango == null || rango.getImporte() == null || rango.getImporte().compareTo(BigDecimal.ZERO) < 0) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "El socio no tiene un rango de cuota social configurado con importe",
                    "Cuota | deSocial | personaId: " + persona.getId());
        }

        Cuota cuota = new Cuota();
        cuota.company = company;
        cuota.persona = persona;
        cuota.titular = resolveTitular(persona);
        cuota.tipo = CuotaTipoEnum.SOCIAL;
        cuota.monto = rango.getImporte();
        cuota.concepto = "Cuota social " + formatMesAnio(fechaGeneracion);
        cuota.aplicarPeriodo(fechaGeneracion, company);
        cuota.aplicarDescuentos(descuentos);
        if (rango.getImporte().compareTo(BigDecimal.ZERO) == 0) {
            cuota.estado = CuotaEstadoEnum.PAGADA;
        }
        return cuota;
    }

    /**
     * Crea la cuota que cobra una membresia (venta, renovacion o cambio de plan). El importe es el precio
     * copiado en la membresia; si es 0 la cuota nace pagada, igual que la cuota social sin importe.
     */
    public static Cuota dePase(
            Company company,
            Membresia membresia,
            LocalDate fechaGeneracion,
            List<Descuento> descuentos) {

        requireNotNull(company, "company");
        requireNotNull(membresia, "membresia");
        requireNotNull(fechaGeneracion, "fechaGeneracion");
        requireMismaCompany(company, membresia.getCompany(), "La membresia no pertenece al club");

        BigDecimal importe = membresia.getPrecio();
        if (importe == null || importe.compareTo(BigDecimal.ZERO) < 0) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "El pase no tiene un precio válido",
                    "Cuota | dePase | membresiaId: " + membresia.getId());
        }

        Cuota cuota = new Cuota();
        cuota.company = company;
        cuota.persona = membresia.getMember();
        cuota.titular = resolveTitular(membresia.getMember());
        cuota.tipo = CuotaTipoEnum.PASE;
        cuota.membresia = membresia;
        cuota.monto = importe;
        String nombrePase = membresia.getPase().getNombre();
        // "Pase libre mensual" no necesita otro "Pase" adelante; "Pack 8 ingresos" sí.
        String prefijo = nombrePase.toLowerCase().startsWith("pase") ? "" : "Pase ";
        cuota.concepto = prefijo + nombrePase + " (" + formatFecha(membresia.getFechaInicio())
                + " al " + formatFecha(membresia.getFechaFin()) + ")";
        cuota.aplicarPeriodo(fechaGeneracion, company);
        cuota.aplicarDescuentos(descuentos);
        if (importe.compareTo(BigDecimal.ZERO) == 0 || cuota.montoFinal().compareTo(BigDecimal.ZERO) == 0) {
            cuota.estado = CuotaEstadoEnum.PAGADA;
        }
        return cuota;
    }

    private static String formatFecha(LocalDate fecha) {
        return String.format("%02d/%02d/%d", fecha.getDayOfMonth(), fecha.getMonthValue(), fecha.getYear());
    }

    /**
     * Agrega un descuento a la cuota como snapshot inmutable. No hace nada si {@code descuento}
     * es {@code null}.
     */
    public void aplicarDescuento(Descuento descuento) {
        if (descuento == null) {
            return;
        }
        descuentos.add(CuotaDescuento.deDescuento(this, descuento));
    }

    /** Conveniencia para aplicar varios descuentos de una vez; ignora los nulos. */
    public void aplicarDescuentos(List<Descuento> descuentos) {
        if (descuentos == null) {
            return;
        }
        descuentos.forEach(this::aplicarDescuento);
    }

    /**
     * Importe realmente cobrable: {@link #monto} menos la suma de {@link #descuentos}, con dos
     * decimales y sin bajar de cero. Es el único lugar donde vive la fórmula en Java — todo
     * cobro, liquidación o total mostrado debe salir de acá y no de {@link #getMonto()}.
     */
    public BigDecimal montoFinal() {
        if (monto == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal totalDescuentos = descuentos.stream()
                .map(CuotaDescuento::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return monto.subtract(totalDescuentos)
                .max(BigDecimal.ZERO)
                .add(recargo != null ? recargo : BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** Política de recargo que rige esta cuota: la del club (social) o la de su actividad. Las membresías no tienen. */
    private RecargoConfig politicaRecargo(Company company) {
        if (esDeActividad()) {
            return actividad != null ? actividad.getRecargo() : null;
        }
        if (esSocial()) {
            return company.getRecargo();
        }
        return null;
    }

    /**
     * Aplica el recargo una única vez si la cuota sigue impaga y ya pasó el vencimiento más los días de gracia
     * de su política (la del club para la cuota social, la de la actividad para las de actividad). Una cuota
     * pagada a término nunca lo recibe. Responde true si lo aplicó.
     */
    public boolean aplicarRecargoSiCorresponde(Company company, LocalDate hoy) {
        RecargoConfig politica = politicaRecargo(company);
        if (recargoAplicadoAt != null || !estaImpaga() || politica == null || !politica.estaActivo()) {
            return false;
        }
        LocalDate desde = politica.getHabilitadoDesde();
        if (desde != null && vencimiento.isBefore(desde)) {
            return false;
        }
        if (!hoy.isAfter(vencimiento.plusDays(politica.diasGraciaEfectivo()))) {
            return false;
        }
        this.recargo = politica.calcular(montoFinal());
        this.recargoAplicadoAt = hoy;
        return true;
    }

    /** Cambia el importe de lista de una cuota impaga sin descuentos ni recargo. Responde true si lo cambió. */
    public boolean actualizarImporteImpago(BigDecimal nuevoImporte) {
        if (!estaImpaga() || tieneDescuento() || tieneRecargo() || recargoAplicadoAt != null) {
            return false;
        }
        this.monto = nuevoImporte;
        return true;
    }

    public boolean tieneRecargo() {
        return recargo != null && recargo.signum() > 0;
    }

    public boolean tieneDescuento() {
        return !descuentos.isEmpty();
    }

    /** Marca la cuota como cobrada. Solo válido desde PENDIENTE o VENCIDA. */
    public void marcarPagada() {
        if (estado == CuotaEstadoEnum.PAGADA) {
            throw new CustomException(HttpStatus.CONFLICT, "La cuota ya está pagada",
                    "Cuota | marcarPagada | uuid: " + getUuid());
        }
        if (estado == CuotaEstadoEnum.ANULADA) {
            throw new CustomException(HttpStatus.CONFLICT, "No se puede pagar una cuota anulada",
                    "Cuota | marcarPagada | uuid: " + getUuid());
        }
        this.estado = CuotaEstadoEnum.PAGADA;
    }

    /**
     * Revierte un cobro y devuelve la cuota a la deuda, recalculando si a {@code hoy} ya
     * está vencida.
     */
    public void revertirPago(LocalDate hoy) {
        if (estado != CuotaEstadoEnum.PAGADA) {
            throw new CustomException(HttpStatus.CONFLICT, "La cuota asociada no está en estado pagada",
                    "Cuota | revertirPago | uuid: " + getUuid() + " estado: " + estado);
        }
        this.estado = vencimiento.isBefore(hoy) ? CuotaEstadoEnum.VENCIDA : CuotaEstadoEnum.PENDIENTE;
    }

    /** Anula la cuota. Una cuota pagada debe desarmarse primero por el movimiento de cobro. */
    public void anular(User porQuien, String motivo) {
        if (estado == CuotaEstadoEnum.ANULADA) {
            throw new CustomException(HttpStatus.CONFLICT, "La cuota ya está anulada",
                    "Cuota | anular | uuid: " + getUuid());
        }
        if (estado == CuotaEstadoEnum.PAGADA) {
            throw new CustomException(HttpStatus.CONFLICT,
                    "No se puede anular una cuota pagada; anule primero el movimiento de cobro asociado",
                    "Cuota | anular | uuid: " + getUuid());
        }
        this.estado = CuotaEstadoEnum.ANULADA;
        this.anuladaPor = porQuien;
        this.anuladaMotivo = motivo != null ? motivo.trim() : null;
        this.anuladaAt = new Timestamp(System.currentTimeMillis());
    }

    public boolean esDeActividad() {
        return tipo == CuotaTipoEnum.ACTIVIDAD;
    }

    public boolean esSocial() {
        return tipo == CuotaTipoEnum.SOCIAL;
    }

    public boolean estaImpaga() {
        return estado == CuotaEstadoEnum.PENDIENTE || estado == CuotaEstadoEnum.VENCIDA;
    }

    private void aplicarPeriodo(LocalDate fechaGeneracion, Company company) {
        this.fechaGeneracion = fechaGeneracion;
        this.estado = CuotaEstadoEnum.PENDIENTE;

        if (this.esDeActividad()) {
            this.vencimiento = actividad.tieneDiaVencimientoFijo()
                    ? proximoDiaDelMes(fechaGeneracion, actividad.getDiaVencimientoFijo())
                    : fechaGeneracion.plusDays(actividad.getDiasVencimientoCuota());
            return;
        }

        this.vencimiento = company.calcularVencimiento(fechaGeneracion);
    }

    /** Primera fecha >= {@code desde} que cae en el dia {@code dia} del mes (dia entre 1 y 28). */
    static LocalDate proximoDiaDelMes(LocalDate desde, int dia) {
        LocalDate candidata = desde.withDayOfMonth(dia);
        return candidata.isBefore(desde) ? candidata.plusMonths(1) : candidata;
    }

    private static Member resolveTitular(Member persona) {
        if (persona.isTitular()) {
            return persona;
        }
        Member titular = persona.getTitular();
        if (titular == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "El socio no tiene titular asociado",
                    "Cuota | resolveTitular | personaId: " + persona.getId());
        }
        return titular;
    }

    private static void requireMismaCompany(Company company, Company otra, String mensaje) {
        if (otra == null || company.getId() == null || !company.getId().equals(otra.getId())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, mensaje,
                    "Cuota | company mismatch | companyId: " + company.getId());
        }
    }

    private static void requireNotNull(Object valor, String nombre) {
        if (valor == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "Faltan datos para generar la cuota",
                    "Cuota | " + nombre + " null");
        }
    }

    private static String formatMesAnio(LocalDate fecha) {
        String mesAnio = fecha.format(MES_ANIO_FORMAT);
        if (mesAnio.isEmpty()) {
            return mesAnio;
        }
        return mesAnio.substring(0, 1).toUpperCase(Locale.ROOT) + mesAnio.substring(1);
    }
}
