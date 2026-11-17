package com.blue.open.Crm.Service;

import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Crm.Config.CrmRestClientConfig;
import com.blue.open.Crm.Entity.DTO.Request.CrmClienteRequestDTO;
import com.blue.open.Crm.Entity.DTO.Request.CrmDescuentoRequestDTO;
import com.blue.open.Crm.Entity.DTO.Request.CrmDesuscripcionRequestDTO;
import com.blue.open.Crm.Entity.DTO.Request.CrmFacturaProductosRequestDTO;
import com.blue.open.Crm.Entity.DTO.Request.CrmItemProductoRequestDTO;
import com.blue.open.Crm.Entity.DTO.Request.CrmSuscripcionRequestDTO;
import com.blue.open.Crm.Entity.DTO.Request.CrmWebhookAckRequestDTO;
import com.blue.open.Crm.Entity.DTO.Request.PagoIntegracionRequestDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmCancelarSuscripcionesResponseDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmClienteResponseDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmCondicionImpositivaResponseDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmDesuscripcionResponseDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmEmpresaExternaResponseDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmEstadoMonetarioResponseDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmFacturaProductosResponseDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmPlanResponseDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmPlanesClienteResponseDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmProductoResponseDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmProvinciaResponseDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmSuscripcionResponseDTO;
import com.blue.open.Crm.Entity.DTO.Response.MercadoPagoPreferenceResponseDTO;
import com.blue.open.Crm.Entity.Enums.CrmTipoDescuentoEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Cliente del modulo de integraciones del CRM (blue-crm).
 * <p>
 * Expone un metodo por cada endpoint publicado por IntegracionesController del CRM:
 * catalogo de planes y productos, alta de clientes, suscripciones, facturacion de pago unico
 * y confirmacion (ACK) de los webhooks recibidos.
 * <p>
 * Depende de: servicio externo blue-crm, alcanzable en crm.base.url y autenticado con
 * crm.api.token. Toda la configuracion se resuelve en CrmRestClientConfig.
 * <p>
 * Manejo de errores: los datos invalidos se rechazan antes de salir a la red con
 * CustomException 400. Las fallas del CRM se traducen a CustomException 502 (BAD_GATEWAY)
 * con el cuerpo de la respuesta del CRM en el developerMessage. Ningun metodo devuelve null:
 * las listas vacias se responden como lista vacia.
 */
@Service
@Slf4j
public class CrmIntegracionService {

    // El CRM exige CUIT de 11 digitos sin guiones
    private static final Pattern PATRON_CUIT = Pattern.compile("^[0-9]{11}$");

    // Rango admitido para el porcentaje de descuento de una suscripcion
    private static final BigDecimal PORCENTAJE_MINIMO = BigDecimal.ONE;
    private static final BigDecimal PORCENTAJE_MAXIMO = BigDecimal.valueOf(100);

    private final RestClient crmRestClient;

    private final CrmRestClientConfig crmRestClientConfig;

    // Constructor explicito (en lugar de @RequiredArgsConstructor) para poder declarar el
    // @Qualifier del RestClient: Lombok no copia esa anotacion al parametro del constructor
    public CrmIntegracionService(
            @Qualifier("crmRestClient") RestClient crmRestClient,
            CrmRestClientConfig crmRestClientConfig) {
        this.crmRestClient = crmRestClient;
        this.crmRestClientConfig = crmRestClientConfig;
    }

    // ! ---------------------------------------------------------------
    // ! Catalogo
    // ! ---------------------------------------------------------------

    /**
     * Obtiene los planes que el CRM publica para este sistema externo.
     * <p>
     * Depende de: GET integraciones/obtener-planes del CRM.
     * Espera: nada.
     * Responde: la lista de planes, vacia si el CRM no devuelve ninguno.
     * <p>
     * Nota: el CRM cachea este endpoint y la cache no distingue empresa.
     */
    public List<CrmPlanResponseDTO> obtenerPlanes() {
        return this.ejecutarLista("obtenerPlanes", "Error al obtener los planes del CRM",
                () -> this.crmRestClient.get()
                        .uri("integraciones/obtener-planes")
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<CrmPlanResponseDTO>>() {
                        }));
    }

    /**
     * Obtiene los productos que el CRM publica para este sistema externo.
     * <p>
     * Depende de: GET integraciones/obtener-productos del CRM.
     * Espera: nada.
     * Responde: la lista de productos, vacia si el CRM no devuelve ninguno.
     * <p>
     * Nota: el CRM cachea este endpoint y la cache no distingue empresa.
     */
    public List<CrmProductoResponseDTO> obtenerProductos() {
        return this.ejecutarLista("obtenerProductos", "Error al obtener los productos del CRM",
                () -> this.crmRestClient.get()
                        .uri("integraciones/obtener-productos")
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<CrmProductoResponseDTO>>() {
                        }));
    }

    /**
     * Obtiene las empresas externas registradas en el CRM.
     * <p>
     * Depende de: GET integraciones/obtener-empresas-externas del CRM.
     * Espera: nada.
     * Responde: la lista de empresas externas, vacia si el CRM no devuelve ninguna.
     */
    public List<CrmEmpresaExternaResponseDTO> obtenerEmpresasExternas() {
        return this.ejecutarLista("obtenerEmpresasExternas", "Error al obtener las empresas externas del CRM",
                () -> this.crmRestClient.get()
                        .uri("integraciones/obtener-empresas-externas")
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<CrmEmpresaExternaResponseDTO>>() {
                        }));
    }

    /**
     * Obtiene el catalogo de provincias del CRM, usado para validar codigoProvincia al dar de
     * alta un club.
     * <p>
     * Depende de: GET clientes/provincias/obtener del CRM. A diferencia del resto de los
     * metodos de esta clase, no vive bajo integraciones/ sino bajo el modulo de clientes del
     * CRM: exige el permiso CLIENTES_PROVINCIAS_VER, otorgado por rol igual que el resto de
     * las autoridades del token de sistema externo.
     * Espera: nada.
     * Responde: la lista de provincias, vacia si el CRM no devuelve ninguna.
     */
    public List<CrmProvinciaResponseDTO> obtenerProvincias() {
        return this.ejecutarLista("obtenerProvincias", "Error al obtener las provincias del CRM",
                () -> this.crmRestClient.get()
                        .uri("clientes/provincias/obtener")
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<CrmProvinciaResponseDTO>>() {
                        }));
    }

    /**
     * Obtiene el catalogo de condiciones impositivas del CRM, usado para validar
     * codigoCondicionImpositiva al dar de alta un club.
     * <p>
     * Depende de: GET clientes/condicion-impositiva/obtener del CRM. Igual que
     * obtenerProvincias, vive bajo el modulo de clientes (permiso
     * CLIENTES_CONDICIONES_IMPOSITIVAS_VER), no bajo integraciones/.
     * Espera: nada.
     * Responde: la lista de condiciones impositivas, vacia si el CRM no devuelve ninguna.
     */
    public List<CrmCondicionImpositivaResponseDTO> obtenerCondicionesImpositivas() {
        return this.ejecutarLista("obtenerCondicionesImpositivas",
                "Error al obtener las condiciones impositivas del CRM",
                () -> this.crmRestClient.get()
                        .uri("clientes/condicion-impositiva/obtener")
                        .retrieve()
                        .body(new ParameterizedTypeReference<List<CrmCondicionImpositivaResponseDTO>>() {
                        }));
    }

    // ! ---------------------------------------------------------------
    // ! Cliente
    // ! ---------------------------------------------------------------

    /**
     * Consulta el estado monetario (deuda) de un cliente en el CRM.
     * <p>
     * Depende de: GET integraciones/obtener-estado-monetario/{cuit} del CRM, que a su vez
     * consulta un servicio BPoint marcado como deprecado.
     * Espera: el CUIT del cliente, 11 digitos sin guiones.
     * Responde: el estado monetario. La lista planes siempre llega vacia, solo se completa deuda.
     */
    public CrmEstadoMonetarioResponseDTO obtenerEstadoMonetario(String cuit) {
        this.validarCuit(cuit, "obtenerEstadoMonetario");
        return this.ejecutar("obtenerEstadoMonetario", "Error al obtener el estado monetario del cliente en el CRM",
                () -> this.crmRestClient.get()
                        .uri("integraciones/obtener-estado-monetario/" + cuit)
                        .retrieve()
                        .body(CrmEstadoMonetarioResponseDTO.class));
    }

    /**
     * Consulta los planes a los que esta suscripto un cliente del CRM.
     * <p>
     * Depende de: GET integraciones/cliente/planes/{cuit} del CRM.
     * Espera: el CUIT del cliente, 11 digitos sin guiones.
     * Responde: el cliente con sus suscripciones vigentes para este sistema externo.
     */
    public CrmPlanesClienteResponseDTO obtenerPlanesCliente(String cuit) {
        this.validarCuit(cuit, "obtenerPlanesCliente");
        return this.ejecutar("obtenerPlanesCliente", "Error al obtener los planes del cliente en el CRM",
                () -> this.crmRestClient.get()
                        .uri("integraciones/cliente/planes/" + cuit)
                        .retrieve()
                        .body(CrmPlanesClienteResponseDTO.class));
    }

    /**
     * Da de alta un cliente en el CRM.
     * <p>
     * Depende de: POST integraciones/cliente/crear del CRM.
     * Espera: los datos fiscales del cliente y su contacto principal.
     * Responde: el uuid del cliente y el estado CLIENTE_CREADO o CLIENTE_EXISTENTE.
     * Si el CUIT ya existia no es un error: se puede continuar con la asociacion de planes.
     */
    public CrmClienteResponseDTO crearCliente(CrmClienteRequestDTO request) {
        this.validarRequestNoNulo(request, "crearCliente");
        this.validarCuit(request.getCuit(), "crearCliente");
        return this.ejecutar("crearCliente", "Error al crear el cliente en el CRM",
                () -> this.crmRestClient.post()
                        .uri("integraciones/cliente/crear")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(CrmClienteResponseDTO.class));
    }

    // ! ---------------------------------------------------------------
    // ! Suscripciones
    // ! ---------------------------------------------------------------

    /**
     * Suscribe un cliente a uno o mas planes del CRM.
     * <p>
     * Depende de: POST integraciones/cliente/suscribir del CRM.
     * Espera: el CUIT, los codigos de plan y opcionalmente la fecha de inicio y el descuento.
     * Responde: las suscripciones creadas con su factura inicial y proxima facturacion.
     * <p>
     * El descuento, si viaja, se aplica a cada una de las suscripciones creadas por separado.
     */
    public CrmSuscripcionResponseDTO suscribirCliente(CrmSuscripcionRequestDTO request) {
        this.validarRequestNoNulo(request, "suscribirCliente");
        this.validarCuit(request.getCuit(), "suscribirCliente");
        this.validarCodigosPlan(request.getCodigosPlan(), "suscribirCliente");
        this.validarDescuento(request.getDescuento());
        return this.ejecutar("suscribirCliente", "Error al suscribir el cliente en el CRM",
                () -> this.crmRestClient.post()
                        .uri("integraciones/cliente/suscribir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(CrmSuscripcionResponseDTO.class));
    }

    /**
     * Da de baja las suscripciones de un cliente a los planes indicados.
     * <p>
     * Depende de: POST integraciones/cliente/desuscribir del CRM.
     * Espera: el CUIT y los codigos de plan a anular.
     * Responde: el CUIT y el mensaje de confirmacion del CRM.
     * Si no hay suscripciones activas que coincidan, el CRM responde 404.
     */
    public CrmDesuscripcionResponseDTO desuscribirCliente(CrmDesuscripcionRequestDTO request) {
        this.validarRequestNoNulo(request, "desuscribirCliente");
        this.validarCuit(request.getCuit(), "desuscribirCliente");
        this.validarCodigosPlan(request.getCodigosPlan(), "desuscribirCliente");
        return this.ejecutar("desuscribirCliente", "Error al desuscribir el cliente en el CRM",
                () -> this.crmRestClient.post()
                        .uri("integraciones/cliente/desuscribir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(CrmDesuscripcionResponseDTO.class));
    }

    /**
     * Cancela todas las suscripciones de un cliente y compensa su deuda pendiente.
     * <p>
     * Depende de: POST integraciones/cliente/cancelar-suscripciones/{cuit} del CRM.
     * Este endpoint no lleva cuerpo pese a ser un POST.
     * Espera: el CUIT del cliente, 11 digitos sin guiones.
     * Responde: la cantidad de suscripciones anuladas, facturas compensadas y monto compensado.
     */
    public CrmCancelarSuscripcionesResponseDTO cancelarSuscripcionesCliente(String cuit) {
        this.validarCuit(cuit, "cancelarSuscripcionesCliente");
        return this.ejecutar("cancelarSuscripcionesCliente", "Error al cancelar las suscripciones del cliente en el CRM",
                () -> this.crmRestClient.post()
                        .uri("integraciones/cliente/cancelar-suscripciones/" + cuit)
                        .retrieve()
                        .body(CrmCancelarSuscripcionesResponseDTO.class));
    }

    /**
     * Valida un descuento de suscripcion antes de enviarlo al CRM.
     * <p>
     * Es publico para que quien arma una suscripcion pueda validarlo antes de tocar el estado del
     * cliente en el CRM (por ejemplo, antes de desuscribirlo de sus planes vigentes).
     * Espera: el descuento, o null si la suscripcion no lleva descuento (null es valido).
     * Responde: nada. Lanza CustomException 400 si el descuento es invalido.
     */
    public void validarDescuento(CrmDescuentoRequestDTO descuento) {
        if (descuento == null) {
            return;
        }
        CrmTipoDescuentoEnum tipoDescuento = descuento.getTipoDescuento();
        if (tipoDescuento == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "El tipo de descuento es obligatorio",
                    "CrmIntegracionService | validarDescuento | tipoDescuento vacio");
        }
        if (tipoDescuento == CrmTipoDescuentoEnum.MONTO && !this.esPositivo(descuento.getMonto())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "El monto del descuento debe ser mayor a cero",
                    "CrmIntegracionService | validarDescuento | monto invalido: " + descuento.getMonto());
        }
        if (tipoDescuento == CrmTipoDescuentoEnum.PORCENTAJE && !this.esPorcentajeValido(descuento.getPorcentaje())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "El porcentaje del descuento debe estar entre 1 y 100",
                    "CrmIntegracionService | validarDescuento | porcentaje invalido: " + descuento.getPorcentaje());
        }
        if (descuento.getCantPeriodos() == null || descuento.getCantPeriodos() < 1) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "La cantidad de periodos del descuento debe ser mayor o igual a 1",
                    "CrmIntegracionService | validarDescuento | cantPeriodos invalido: " + descuento.getCantPeriodos());
        }
    }

    // ! ---------------------------------------------------------------
    // ! Facturacion
    // ! ---------------------------------------------------------------

    /**
     * Genera en el CRM una factura de productos (pago unico, sin suscripcion).
     * <p>
     * Depende de: POST integraciones/facturas/productos del CRM.
     * Espera: el CUIT y la lista de productos con sus cantidades.
     * Responde: la factura generada con su monto, estado e items.
     */
    public CrmFacturaProductosResponseDTO crearFacturaProductos(CrmFacturaProductosRequestDTO request) {
        this.validarRequestNoNulo(request, "crearFacturaProductos");
        this.validarCuit(request.getCuit(), "crearFacturaProductos");
        this.validarItemsProducto(request.getItems());
        return this.ejecutar("crearFacturaProductos", "Error al generar la factura de productos en el CRM",
                () -> this.crmRestClient.post()
                        .uri("integraciones/facturas/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(CrmFacturaProductosResponseDTO.class));
    }

    /**
     * Crea en el CRM un pago para una o mas facturas de un cliente y devuelve el link de pago.
     * <p>
     * Depende de: POST integraciones/pagos/crear del CRM.
     * Espera: los uuids de factura DEL CRM (Factura.uuidFacturaCrm de este lado, no el uuid
     * local), el CUIT del cliente, una referencia externa y la fecha de pago.
     * Responde: el id y el init_point de la preferencia de Mercado Pago creada por el CRM.
     */
    public MercadoPagoPreferenceResponseDTO crearPago(PagoIntegracionRequestDTO request) {
        this.validarRequestNoNulo(request, "crearPago");
        this.validarCuit(request.getCuit(), "crearPago");
        this.validarFacturasUuid(request.getFacturasUuid(), "crearPago");
        return this.ejecutar("crearPago", "Error al crear el pago en el CRM",
                () -> this.crmRestClient.post()
                        .uri("integraciones/pagos/crear")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .body(MercadoPagoPreferenceResponseDTO.class));
    }

    // ! ---------------------------------------------------------------
    // ! Webhooks
    // ! ---------------------------------------------------------------

    /**
     * Confirma al CRM el procesamiento de un webhook recibido.
     * <p>
     * Depende de: POST integraciones/webhooks/pagos/ack del CRM.
     * Espera: el eventId y el tipo del evento recibido. Para PAGO_IMPUTADO, data debe incluir
     * el numeroFactura y coincidir con el que el CRM guardo.
     * Responde: nada. El CRM contesta con el cuerpo vacio.
     * <p>
     * Si no se envia el ACK, el CRM reintenta el webhook con backoff hasta 10 veces.
     */
    public void ackWebhook(CrmWebhookAckRequestDTO request) {
        this.validarRequestNoNulo(request, "ackWebhook");
        if (request.getEventId() == null || request.getEventId().isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "El evento del CRM a confirmar es obligatorio",
                    "CrmIntegracionService | ackWebhook | eventId vacio");
        }
        if (request.getTipo() == null || request.getTipo().isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "El tipo de evento del CRM a confirmar es obligatorio",
                    "CrmIntegracionService | ackWebhook | tipo vacio | eventId: " + request.getEventId());
        }

        this.ejecutar("ackWebhook", "Error al confirmar el evento del CRM",
                () -> this.crmRestClient.post()
                        .uri("integraciones/webhooks/pagos/ack")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .toBodilessEntity());
    }

    // ! ---------------------------------------------------------------
    // ! Infraestructura interna
    // ! ---------------------------------------------------------------

    /**
     * Ejecuta una llamada al CRM centralizando la validacion de configuracion, el logueo y
     * la traduccion de errores a CustomException.
     * <p>
     * Espera: el nombre del metodo llamador (para logs), el mensaje de error para el usuario
     * final y la llamada al CRM.
     * Responde: lo que devuelva la llamada.
     */
    private <T> T ejecutar(String metodo, String mensajeUsuario, Supplier<T> llamada) {
        this.validarConfiguracion(metodo, mensajeUsuario);
        try {
            T respuesta = llamada.get();
            log.info("CrmIntegracionService | {} | ok", metodo);
            return respuesta;
        } catch (CustomException e) {
            throw e;
        } catch (RestClientResponseException e) {
            log.error("CrmIntegracionService | {} | status={} | body={}",
                    metodo, e.getStatusCode(), e.getResponseBodyAsString());
            throw new CustomException(HttpStatus.BAD_GATEWAY, mensajeUsuario,
                    "CrmIntegracionService | " + metodo + " | status=" + e.getStatusCode()
                            + " | " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("CrmIntegracionService | {} | error: {}", metodo, e.getMessage(), e);
            throw new CustomException(HttpStatus.BAD_GATEWAY, mensajeUsuario,
                    "CrmIntegracionService | " + metodo + " | error: " + e.getMessage());
        }
    }

    /**
     * Variante de ejecutar para los endpoints que devuelven listas.
     * Garantiza que nunca se propague null hacia el llamador.
     */
    private <T> List<T> ejecutarLista(String metodo, String mensajeUsuario, Supplier<List<T>> llamada) {
        List<T> respuesta = this.ejecutar(metodo, mensajeUsuario, llamada);
        return respuesta != null ? respuesta : List.of();
    }

    /**
     * Corta la llamada si faltan las properties necesarias para hablar con el CRM.
     * La aplicacion arranca igual sin configuracion de CRM, por eso la validacion se hace
     * en cada llamada y no en el arranque.
     */
    private void validarConfiguracion(String metodo, String mensajeUsuario) {
        if (!this.crmRestClientConfig.isConfiguracionCompleta()) {
            throw new CustomException(HttpStatus.SERVICE_UNAVAILABLE, mensajeUsuario,
                    "CrmIntegracionService | " + metodo + " | integracion con el CRM sin configurar: "
                            + this.crmRestClientConfig.getDetalleConfiguracionFaltante());
        }
    }

    // Valida el formato de CUIT antes de salir a la red. Ademas garantiza que el valor sea
    // seguro para concatenar en la uri, ya que solo admite digitos
    private void validarCuit(String cuit, String metodo) {
        if (cuit == null || !PATRON_CUIT.matcher(cuit.trim()).matches()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "El CUIT debe tener 11 dígitos numéricos",
                    "CrmIntegracionService | " + metodo + " | cuit invalido: " + cuit);
        }
    }

    private void validarRequestNoNulo(Object request, String metodo) {
        if (request == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Faltan los datos para comunicarse con el CRM",
                    "CrmIntegracionService | " + metodo + " | request nulo");
        }
    }

    // Replica la validacion del CRM para fallar antes con un mensaje mas claro
    private void validarFacturasUuid(List<String> facturasUuid, String metodo) {
        if (facturasUuid == null || facturasUuid.isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Debe indicar al menos una factura",
                    "CrmIntegracionService | " + metodo + " | facturasUuid vacio");
        }
    }

    // Replica la validacion del CRM para fallar antes con un mensaje mas claro
    private void validarCodigosPlan(List<String> codigosPlan, String metodo) {
        if (codigosPlan == null || codigosPlan.isEmpty()
                || codigosPlan.stream().allMatch(codigo -> codigo == null || codigo.isBlank())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Debe indicar al menos un plan",
                    "CrmIntegracionService | " + metodo + " | codigosPlan vacio");
        }
    }

    private boolean esPositivo(BigDecimal valor) {
        return valor != null && valor.signum() > 0;
    }

    private boolean esPorcentajeValido(BigDecimal porcentaje) {
        return porcentaje != null
                && porcentaje.compareTo(PORCENTAJE_MINIMO) >= 0
                && porcentaje.compareTo(PORCENTAJE_MAXIMO) <= 0;
    }

    // Replica la validacion del CRM para fallar antes con un mensaje mas claro
    private void validarItemsProducto(List<CrmItemProductoRequestDTO> items) {
        if (items == null || items.isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Debe indicar al menos un producto",
                    "CrmIntegracionService | crearFacturaProductos | items vacio");
        }
        for (CrmItemProductoRequestDTO item : items) {
            if (item == null || item.getCodigoProducto() == null || item.getCodigoProducto().isBlank()) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "El código de producto es obligatorio",
                        "CrmIntegracionService | crearFacturaProductos | codigoProducto vacio");
            }
            if (item.getCantidad() != null && item.getCantidad() <= 0) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "La cantidad del producto debe ser mayor a cero",
                        "CrmIntegracionService | crearFacturaProductos | cantidad invalida: " + item.getCantidad()
                                + " | codigoProducto: " + item.getCodigoProducto());
            }
        }
    }
}
