package com.blue.open.Code.Entity.Db;

import com.blue.open.Code.Entity.Enums.CertificadoVigenciaModoEnum;
import com.blue.open.Code.Entity.Enums.PeriodicidadCuotaEnum;
import com.blue.open.Core.Entity.Db.BaseCompany;
import com.blue.open.Core.Exceptions.CustomException;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.http.HttpStatus;

import java.sql.Types;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Locale;

@Setter
@Getter
@Table(name = "companies")
@Entity()
@AttributeOverride(name = "path", column = @Column(name = "path", length = 256, unique = true))
public class Company extends BaseCompany {

    @Column(name = "cuit", length = 20)
    private String cuit;

    @Column(name = "nombre_comercial")
    private String nombreComercial;

    @Column(name = "razon_social")
    private String razonSocial;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "direccion", column = @Column(name = "domicilio_direccion", length = 512)),
            @AttributeOverride(name = "codigoPostal", column = @Column(name = "domicilio_codigo_postal", length = 16)),
            @AttributeOverride(name = "localidad", column = @Column(name = "domicilio_localidad", length = 128))
    })
    private CompanyDireccion domicilio;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "direccion", column = @Column(name = "domicilio_facturacion_direccion", length = 512)),
            @AttributeOverride(name = "codigoPostal", column = @Column(name = "domicilio_facturacion_codigo_postal", length = 16)),
            @AttributeOverride(name = "localidad", column = @Column(name = "domicilio_facturacion_localidad", length = 128))
    })
    private CompanyDireccion domicilioFacturacion;

    @Column(name = "telefono", length = 50)
    private String telefono;

    @Column(name = "email_factura_electronica")
    private String emailFacturaElectronica;

    @Column(name = "email_institucional")
    private String emailInstitucional;

    @Column(name = "logo_url", length = 512)
    private String logoUrl;

    @Column(name = "color_primario", length = 32)
    private String colorPrimario;

    @Column(name = "color_secundario", length = 32)
    private String colorSecundario;

    @Column(name = "cuota_social_habilitada")
    private Boolean cuotaSocialHabilitada;

    /** Cada cuánto se genera la cuota social. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "periodicidad_cuota_social", nullable = false, length = 20)
    private PeriodicidadCuotaEnum periodicidadCuotaSocial = PeriodicidadCuotaEnum.MENSUAL;

    /** Días desde la generación hasta el vencimiento de la cuota social. */
    @Column(name = "dias_vencimiento_cuota", nullable = false)
    private Integer diasVencimientoCuota = 10;

    @Column(name = "dias_vencimiento_moroso", nullable = false)
    private Integer diasVencimientoMoroso = 31;

    @Column(name = "dias_inhabilitacion", nullable = false)
    private Integer diasInhabilitacion = 91;

    @Column(name = "admin_creado")
    private Boolean adminCreado;

    /** Cómo vence el certificado médico: día fijo del año para todos, o N meses desde la carga. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "certificado_vigencia_modo", nullable = false, length = 20)
    private CertificadoVigenciaModoEnum certificadoVigenciaModo = CertificadoVigenciaModoEnum.FECHA_FIJA;

    @Column(name = "certificado_vigencia_meses", nullable = false)
    private Integer certificadoVigenciaMeses = 12;

    /** Con FECHA_FIJA: día y mes en que todos renuevan (1/1 = el certificado vence el 31/12). */
    @Column(name = "certificado_renovacion_dia", nullable = false)
    private Integer certificadoRenovacionDia = 1;

    @Column(name = "certificado_renovacion_mes", nullable = false)
    private Integer certificadoRenovacionMes = 1;

    /** Recargo por pago fuera de término de la cuota social. Cada actividad tiene el suyo. */
    @Embedded
    private RecargoConfig recargo = new RecargoConfig();

    // Identificador del cliente en el CRM (blue-crm). Lo carga el alta parcial desde ALTA_CLIENTE
    @Column(name = "uuid_cliente", length = 64, unique = true)
    private String uuidCliente;

    // false hasta que el club completa el onboarding. Las Companies existentes quedan en true
    @Column(name = "onboarding_finalizado", nullable = false)
    private Boolean onboardingFinalizado = false;

    @OneToOne(mappedBy = "company", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private CompanyMercadoPago mercadoPago;

    // LAZY a proposito: solo lo consulta CompanyAccesoService, no hace falta cargarlo cada vez
    // que se trae un Company. No navegar este getter fuera de una transaccion.
    @OneToOne(mappedBy = "company", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private CompanyBloqueoAcceso bloqueoAcceso;

    private static final int DEFAULT_DIAS_VENCIMIENTO_CUOTA = 10;
    private static final int DEFAULT_DIAS_VENCIMIENTO_MOROSO = 31;
    private static final int DEFAULT_DIAS_INHABILITACION = 91;

    public int diasVencimientoCuotaEfectivo() {
        return diasVencimientoCuota != null && diasVencimientoCuota > 0
                ? diasVencimientoCuota
                : DEFAULT_DIAS_VENCIMIENTO_CUOTA;
    }

    public int diasVencimientoMorosoEfectivo() {
        return diasVencimientoMoroso != null && diasVencimientoMoroso > 0
                ? diasVencimientoMoroso
                : DEFAULT_DIAS_VENCIMIENTO_MOROSO;
    }

    public int diasInhabilitacionEfectivo() {
        return diasInhabilitacion != null && diasInhabilitacion > 0
                ? diasInhabilitacion
                : DEFAULT_DIAS_INHABILITACION;
    }

    /** Vencimiento de la cuota social. La morosidad del grupo usa {@link #diasVencimientoMoroso}, no esto. */
    public LocalDate calcularVencimiento(LocalDate fechaGeneracion) {
        return fechaGeneracion.plusDays(diasVencimientoCuotaEfectivo());
    }

    /** Último día de vigencia de un certificado cargado en {@code fechaCarga}, según la configuración del club. */
    public LocalDate calcularVencimientoCertificado(LocalDate fechaCarga) {
        if (certificadoVigenciaModo == CertificadoVigenciaModoEnum.DURACION) {
            int meses = certificadoVigenciaMeses != null && certificadoVigenciaMeses > 0 ? certificadoVigenciaMeses : 12;
            return fechaCarga.plusMonths(meses);
        }
        int mes = certificadoRenovacionMes != null && certificadoRenovacionMes >= 1 && certificadoRenovacionMes <= 12
                ? certificadoRenovacionMes : 1;
        int dia = certificadoRenovacionDia != null && certificadoRenovacionDia >= 1 ? certificadoRenovacionDia : 1;
        LocalDate renovacion = fechaRenovacion(fechaCarga.getYear(), mes, dia);
        if (!renovacion.isAfter(fechaCarga)) {
            renovacion = fechaRenovacion(fechaCarga.getYear() + 1, mes, dia);
        }
        return renovacion.minusDays(1);
    }

    private static LocalDate fechaRenovacion(int anio, int mes, int dia) {
        int ultimoDia = java.time.YearMonth.of(anio, mes).lengthOfMonth();
        return LocalDate.of(anio, mes, Math.min(dia, ultimoDia));
    }

    /** Actualiza la configuración del certificado; los parámetros null conservan el valor actual. */
    public void configurarCertificado(CertificadoVigenciaModoEnum modo, Integer meses, Integer dia, Integer mes) {
        if (meses != null) {
            if (meses < 1 || meses > 120) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "Duración del certificado inválida",
                        "La duración del certificado debe estar entre 1 y 120 meses.");
            }
            this.certificadoVigenciaMeses = meses;
        }
        if (mes != null) {
            if (mes < 1 || mes > 12) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "Mes de renovación inválido",
                        "El mes de renovación del certificado debe estar entre 1 y 12.");
            }
            this.certificadoRenovacionMes = mes;
        }
        if (dia != null) {
            if (dia < 1 || dia > 31) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "Día de renovación inválido",
                        "El día de renovación del certificado debe estar entre 1 y 31.");
            }
            this.certificadoRenovacionDia = dia;
        }
        if (modo != null) {
            this.certificadoVigenciaModo = modo;
        }
    }

    public RecargoConfig getRecargo() {
        if (recargo == null) {
            recargo = new RecargoConfig();
        }
        return recargo;
    }

    public boolean tieneCuotaSocialConfigurada() {
        return Boolean.TRUE.equals(cuotaSocialHabilitada);
    }

    /**
     * Crea el club parcial a partir del ALTA_CLIENTE del CRM, con el onboarding pendiente.
     * Nace con Mercado Pago deshabilitado y el acceso desbloqueado, igual que el alta por sysadmin.
     * El path se arma desde la razon social (generarPath); si ya esta en uso, quien persiste el
     * club le agrega el sufijo.
     * Responde: la company lista para persistir.
     * Lanza CustomException si falta alguno de los datos, todos obligatorios.
     */
    public static Company crearDesdeAltaCliente(String uuidCliente, String razonSocial, String cuit,
                                                String emailFacturaElectronica) {
        validarObligatorio(uuidCliente, "uuidCliente");
        validarObligatorio(razonSocial, "razonSocial");
        validarObligatorio(cuit, "cuit");
        validarObligatorio(emailFacturaElectronica, "correoFacturacion");

        Company company = new Company();
        company.uuidCliente = uuidCliente;
        String path = generarPath(razonSocial, cuit);

        company.setName(razonSocial);
        company.setPath(path);
        company.razonSocial = razonSocial;
        company.cuit = cuit;
        company.emailFacturaElectronica = emailFacturaElectronica;
        company.onboardingFinalizado = false;
        company.mercadoPago = CompanyMercadoPago.crearDeshabilitadoVacias(company);
        company.bloqueoAcceso = CompanyBloqueoAcceso.crearDesbloqueada(company);
        return company;
    }

    /** Deja constancia de que el club ya tiene su usuario administrador. */
    public void marcarAdminCreado() {
        this.adminCreado = Boolean.TRUE;
    }

    /**
     * Arma el path del club desde la razon social: sin tildes, en minusculas, sin puntos y con
     * guiones en lugar de cualquier otro caracter que no sea letra o numero.
     * Ej.: "Club Atlético Norte S.A." → "club-atletico-norte-sa".
     * Responde el CUIT si de la razon social no queda ningun caracter valido.
     */
    private static String generarPath(String razonSocial, String cuit) {
        String sinTildes = Normalizer.normalize(razonSocial, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String path = sinTildes.toLowerCase(Locale.ROOT)
                .replace(".", "")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");

        if (path.isEmpty()) {
            return cuit;
        }
        return path;
    }

    private static void validarObligatorio(String valor, String campo) {
        if (valor != null && !valor.isBlank()) {
            return;
        }
        throw new CustomException(HttpStatus.BAD_REQUEST, "Falta el campo " + campo + " del alta de cliente",
                "Company | crearDesdeAltaCliente | campo vacio: " + campo);
    }
}
