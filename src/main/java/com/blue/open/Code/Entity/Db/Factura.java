package com.blue.open.Code.Entity.Db;

import com.blue.open.Code.Entity.Enums.FacturaEstadoEnum;
import com.blue.open.Core.Entity.Db.AbstractEntity;
import com.blue.open.Core.Exceptions.CustomException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Factura de un club, generada a partir del evento DEUDA_GENERADA del CRM (blue-crm).
 * <p>
 * Se identifica con el club por el uuidCliente del CRM y, para eventos anteriores, por el CUIT.
 * La relacion con Company es opcional: si la deuda llega antes de que el club exista en esta
 * aplicacion, la factura se registra igual sin club y se asocia cuando se lo da de alta
 * (asociarCompany). El
 * uuidFacturaCrm es el identificador de la factura del lado del CRM y es unico, lo que da la
 * idempotencia frente a los reintentos del webhook.
 * <p>
 * La entidad es duena de su maquina de estados: el estado, las fechas y los codigos no tienen
 * setter publico. Nace siempre en PENDIENTE a traves de crear, y solo se mueve con
 * marcarPagada y marcarVencida.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "facturas")
public class Factura extends AbstractEntity {

    // Dias que se le dan a una factura para pagarse antes de considerarla vencida
    private static final int DIAS_PARA_VENCER = 5;

    // Separador con el que se guardan los codigos de producto en una sola columna
    private static final String SEPARADOR_CODIGOS = ",";

    // Null mientras el club del CUIT no este dado de alta en esta aplicacion
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    // CUIT del club, solo digitos. Lo informaba el CRM hasta pasar a uuidCliente; en los eventos
    // nuevos se toma del club si existe, y si no queda null
    @Column(name = "cuit", length = 20)
    private String cuit;

    // Identificador del cliente en el CRM. Junto con el CUIT, permite asociar la factura al club
    // cuando se lo da de alta
    @Column(name = "uuid_cliente", length = 64)
    private String uuidCliente;

    // Uuid de la factura en el CRM. Es la clave de la idempotencia, por eso es unico
    @Column(name = "uuid_factura_crm", nullable = false, length = 64, unique = true)
    private String uuidFacturaCrm;

    // Codigo del plan que origino la deuda. Puede ser null: una deuda puede venir solo de
    // productos, y en ese caso el CRM manda la lista de planes vacia
    @Column(name = "codigo_plan", length = 60)
    private String codigoPlan;

    // Codigos de los productos que originaron la deuda, unidos por coma
    @Column(name = "codigos_productos", length = 500)
    private String codigosProductos;

    @Column(name = "fecha_generacion", nullable = false)
    private LocalDate fechaGeneracion;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "estado", nullable = false, length = 20)
    private FacturaEstadoEnum estado;

    // Importe total de la factura informado por el CRM (con IVA y descuento aplicados). Si el
    // CRM no lo informa, FacturaService lo calcula sumando el precio del plan y de los productos
    // contra plan_crm/productos_crm; en ese caso puede quedar null si ninguno de esos codigos
    // estaba sincronizado todavia
    @Column(name = "importe", precision = 12, scale = 2)
    private BigDecimal importe;

    // ! ---------------------------------------------------------------
    // ! Construccion
    // ! ---------------------------------------------------------------

    /**
     * Crea una factura a partir de los datos de una deuda generada en el CRM.
     * <p>
     * Nace siempre en PENDIENTE, con la fecha de hoy como generacion y el vencimiento a
     * DIAS_PARA_VENCER dias.
     * <p>
     * De la lista de planes se toma solo el primero: esta aplicacion admite un unico plan por
     * club, asi que el resto se descarta. Si la lista viene vacia, el codigo de plan queda null.
     * <p>
     * Espera: el club al que pertenece (puede ser null si todavia no esta dado de alta), el
     * uuidCliente y el CUIT del cliente (alcanza con uno de los dos), el uuid de la factura en el
     * CRM, los codigos de planes y productos que informo el evento, y el importe ya resuelto por
     * FacturaService (el importeTotal del CRM o, si no vino, la suma de precios contra
     * plan_crm/productos_crm) — no le corresponde a la entidad consultar esas tablas.
     * Responde: la factura lista para persistir.
     * Lanza CustomException si faltan a la vez el uuidCliente y el CUIT, o si falta el uuid del
     * CRM, para que no se pueda construir una factura invalida sin importar quien la llame.
     */
    public static Factura crear(
            Company company,
            String uuidCliente,
            String cuit,
            String uuidFacturaCrm,
            List<String> codigosPlanes,
            List<String> codigosProductos,
            BigDecimal importe) {
        String uuidClienteLimpio = limpiarUuidCliente(uuidCliente);
        String cuitNormalizado = normalizarCuit(cuit);
        if (uuidClienteLimpio == null && cuitNormalizado == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "La factura del CRM no identifica al cliente",
                    "Factura | crear | sin uuidCliente ni cuit | uuidFacturaCrm: " + uuidFacturaCrm);
        }
        if (uuidFacturaCrm == null || uuidFacturaCrm.isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "La factura del CRM no tiene identificador",
                    "Factura | crear | uuidFacturaCrm vacio | uuidCliente: " + uuidClienteLimpio
                            + " | cuit: " + cuitNormalizado);
        }

        LocalDate fechaGeneracion = LocalDate.now();

        Factura factura = new Factura();
        factura.company = company;
        factura.uuidCliente = uuidClienteLimpio;
        factura.cuit = cuitNormalizado;
        factura.uuidFacturaCrm = uuidFacturaCrm.trim();
        factura.codigoPlan = primerCodigoPlan(codigosPlanes);
        factura.codigosProductos = unirCodigos(codigosProductos);
        factura.fechaGeneracion = fechaGeneracion;
        factura.fechaVencimiento = fechaGeneracion.plusDays(DIAS_PARA_VENCER);
        factura.estado = FacturaEstadoEnum.PENDIENTE;
        factura.importe = importe;
        return factura;
    }

    // ! ---------------------------------------------------------------
    // ! Asociacion con el club
    // ! ---------------------------------------------------------------

    /**
     * Asocia la factura al club, para las que se registraron antes de que el club existiera.
     * <p>
     * No lanza: se usa sobre lotes y una factura que no corresponde asociar no tiene que cortar
     * al resto. Una factura nunca cambia de club, asi que si ya tiene uno no se toca.
     * <p>
     * Responde: true si quedo asociada al club indicado; false si no habia club o si la factura
     * ya pertenecia a uno.
     */
    public boolean asociarCompany(Company company) {
        if (company == null || this.company != null) {
            return false;
        }
        this.company = company;
        return true;
    }

    // ! ---------------------------------------------------------------
    // ! Transiciones de estado
    // ! ---------------------------------------------------------------

    /** Marca la factura como cobrada. */
    public void marcarPagada() {
        this.estado = FacturaEstadoEnum.PAGADA;
    }

    /** Marca la factura como vencida por falta de pago. */
    public void marcarVencida() {
        this.estado = FacturaEstadoEnum.VENCIDA;
    }

    // ! ---------------------------------------------------------------
    // ! Preguntas de dominio
    // ! ---------------------------------------------------------------

    /**
     * Indica si la factura paso su fecha de vencimiento.
     * <p>
     * Compara contra la fecha guardada y no contra un recalculo, asi prorrogar una factura es
     * cuestion de cambiarle el campo.
     * <p>
     * Responde: true si hoy es posterior al vencimiento.
     */
    public boolean estaVencida() {
        if (this.fechaVencimiento == null) {
            return false;
        }
        return LocalDate.now().isAfter(this.fechaVencimiento);
    }

    // ! ---------------------------------------------------------------
    // ! Infraestructura interna
    // ! ---------------------------------------------------------------

    /**
     * Devuelve el primer codigo de plan no vacio de la lista, o null si no hay ninguno.
     * Publico: FacturaService lo reusa para resolver el precio del plan con el mismo criterio
     * con el que esta clase fija codigoPlan, sin duplicar la logica.
     */
    public static String primerCodigoPlan(List<String> codigosPlanes) {
        if (codigosPlanes == null || codigosPlanes.isEmpty()) {
            return null;
        }
        return codigosPlanes.stream()
                .filter(codigo -> codigo != null && !codigo.isBlank())
                .map(String::trim)
                .findFirst()
                .orElse(null);
    }

    /**
     * Deja el CUIT solo con digitos, o null si no queda ninguno. El club lo admite con guiones y
     * el CRM lo informa sin ellos; guardarlo normalizado permite cruzarlos.
     * Publico: FacturaService lo reusa para buscar las facturas de un club con el mismo criterio.
     */
    public static String normalizarCuit(String cuit) {
        if (cuit == null) {
            return null;
        }
        String digitos = cuit.replaceAll("[^0-9]", "");
        return digitos.isEmpty() ? null : digitos;
    }

    /** Devuelve el uuidCliente sin espacios, o null si viene vacio. */
    private static String limpiarUuidCliente(String uuidCliente) {
        if (uuidCliente == null || uuidCliente.isBlank()) {
            return null;
        }
        return uuidCliente.trim();
    }

    /** Une los codigos en una sola cadena separada por comas, o null si no hay ninguno. */
    private static String unirCodigos(List<String> codigos) {
        if (codigos == null || codigos.isEmpty()) {
            return null;
        }

        String unidos = codigos.stream()
                .filter(codigo -> codigo != null && !codigo.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(SEPARADOR_CODIGOS));

        return unidos.isBlank() ? null : unidos;
    }
}
