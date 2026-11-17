package com.blue.open.Crm.Config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

/**
 * Configuracion del cliente HTTP contra el CRM (blue-crm).
 * <p>
 * Construye el bean "crmRestClient" que usa CrmIntegracionService para consumir el modulo
 * de integraciones del CRM. Centraliza la url base, el token de autenticacion y el
 * ObjectMapper propio del cliente.
 * <p>
 * Variables de entorno (convencion: admiten null y se validan manualmente):
 * - crm.base.url: url base del CRM, ej https://stg.blueopendata.com/blue-crm/api/
 * - crm.api.token: JWT de larga duracion emitido por el CRM al crear el sistema externo.
 * El CRM lo compara byte a byte contra integraciones_sistemas_externos.token, por lo que
 * no sirve generar uno nuevo por /auth/integration/login.
 * <p>
 * Si falta alguna de las dos properties NO se corta el arranque de la aplicacion (el club
 * funciona sin CRM): se loguea el error y CrmIntegracionService rechaza cada llamada con
 * un CustomException explicando la property faltante.
 */
@Configuration
@Slf4j
public class CrmRestClientConfig {

    @Value("${crm.base.url:#{null}}")
    private String baseUrl;

    @Value("${crm.api.token:#{null}}")
    private String apiToken;

    // Deja en crm_integracion_logs una fila por cada llamada saliente al CRM
    private final CrmLoggingInterceptor crmLoggingInterceptor;

    public CrmRestClientConfig(CrmLoggingInterceptor crmLoggingInterceptor) {
        this.crmLoggingInterceptor = crmLoggingInterceptor;
    }

    /**
     * Crea el RestClient apuntando al CRM con el header Authorization ya configurado.
     * <p>
     * Depende de: properties crm.base.url y crm.api.token.
     * Responde: un RestClient listo para usar. Si la configuracion esta incompleta devuelve
     * igualmente un RestClient (sin baseUrl ni token) para no romper el contexto de Spring;
     * las llamadas quedan bloqueadas por validarConfiguracion() en el service.
     */
    @Bean("crmRestClient")
    public RestClient crmRestClient() {
        if (!this.isConfiguracionCompleta()) {
            log.error("CrmRestClientConfig | crmRestClient | configuracion incompleta: {}",
                    this.getDetalleConfiguracionFaltante());
            return RestClient.builder()
                    .messageConverters(this::configurarConverters)
                    .build();
        }

        log.info("CrmRestClientConfig | crmRestClient | CRM configurado en {}", this.getBaseUrlNormalizada());
        return RestClient.builder()
                .baseUrl(this.getBaseUrlNormalizada())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + this.apiToken.trim())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .messageConverters(this::configurarConverters)
                .requestFactory(this.crearRequestFactory())
                .requestInterceptor(this.crmLoggingInterceptor)
                .build();
    }

    /**
     * Crea el factory de requests envolviendo en buffering al que resuelve Spring Boot.
     * <p>
     * El buffering es necesario para que CrmLoggingInterceptor pueda leer el cuerpo de la
     * respuesta: sin el, leerlo consumiria el stream y el RestClient recibiria un body vacio.
     * <p>
     * Se envuelve el factory que elige Boot y no uno fijo porque el proyecto tiene Apache
     * HttpClient5 en el classpath: Boot lo autodetecta y usa pooling de conexiones. Fijar un
     * SimpleClientHttpRequestFactory a mano degradaria el stack HTTP en silencio.
     */
    private ClientHttpRequestFactory crearRequestFactory() {
        return new BufferingClientHttpRequestFactory(
                ClientHttpRequestFactories.get(ClientHttpRequestFactorySettings.DEFAULTS));
    }

    /**
     * Indica si estan presentes las dos properties necesarias para hablar con el CRM.
     * Lo consultan CrmIntegracionService (antes de cada llamada) y CrmSmokeTestService.
     */
    public boolean isConfiguracionCompleta() {
        return this.baseUrl != null && !this.baseUrl.isBlank()
                && this.apiToken != null && !this.apiToken.isBlank();
    }

    /**
     * Devuelve el nombre de las properties que faltan, para incluirlo en logs y en el
     * developerMessage de los errores. Responde un texto vacio si no falta ninguna.
     */
    public String getDetalleConfiguracionFaltante() {
        StringBuilder detalle = new StringBuilder();
        if (this.baseUrl == null || this.baseUrl.isBlank()) {
            detalle.append("falta crm.base.url");
        }
        if (this.apiToken == null || this.apiToken.isBlank()) {
            if (detalle.length() > 0) {
                detalle.append(" | ");
            }
            detalle.append("falta crm.api.token");
        }
        return detalle.toString();
    }

    /**
     * Normaliza la url base para que siempre termine en barra, ya que las uri del service
     * son relativas y no llevan barra inicial.
     */
    private String getBaseUrlNormalizada() {
        String url = this.baseUrl.trim();
        return url.endsWith("/") ? url : url + "/";
    }

    /**
     * Reemplaza los converters por defecto por uno solo de Jackson con ObjectMapper propio.
     * <p>
     * Motivo: JacksonConfig del Core define un ObjectMapper global; al tener el cliente su
     * propio mapper, la serializacion contra el CRM queda aislada de cambios en el global.
     */
    private void configurarConverters(java.util.List<org.springframework.http.converter.HttpMessageConverter<?>> converters) {
        converters.clear();
        converters.add(new MappingJackson2HttpMessageConverter(this.crmObjectMapper()));
    }

    /**
     * ObjectMapper exclusivo del cliente CRM.
     * - JavaTimeModule para los tipos java.time.
     * - FAIL_ON_UNKNOWN_PROPERTIES desactivado: el CRM puede agregar campos nuevos.
     * - NON_NULL: no se mandan campos nulos en los request.
     */
    private ObjectMapper crmObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return objectMapper;
    }
}
