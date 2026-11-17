package com.blue.open.Crm.Service;

import com.blue.open.Crm.Config.CrmRestClientConfig;
import com.blue.open.Crm.Entity.DTO.Response.CrmProductoResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Smoke test de la integracion con el CRM (blue-crm).
 * <p>
 * Al terminar de levantar la aplicacion verifica que el CRM este configurado y responda,
 * haciendo una consulta de solo lectura al catalogo de productos.
 * <p>
 * Depende de: CrmIntegracionService y, a traves de el, del servicio externo blue-crm.
 * Espera: nada.
 * Responde: nada. El resultado se informa unicamente por log.
 * <p>
 * No corta el arranque ante una falla: el club funciona sin CRM, por lo que una caida del
 * CRM no debe impedir que la aplicacion levante.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CrmSmokeTestService {

    private final CrmIntegracionService crmIntegracionService;

    private final CrmRestClientConfig crmRestClientConfig;

    /**
     * Ejecuta la verificacion de conectividad contra el CRM una vez que el contexto esta listo.
     * Loguea el detalle de la property faltante si la integracion no esta configurada, o el
     * error devuelto por el CRM si la llamada falla.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void verificarConexionCrm() {
        if (!this.crmRestClientConfig.isConfiguracionCompleta()) {
            log.error("CrmSmokeTestService | verificarConexionCrm | integracion con el CRM sin configurar: {}",
                    this.crmRestClientConfig.getDetalleConfiguracionFaltante());
            return;
        }

        try {
            List<CrmProductoResponseDTO> productos = this.crmIntegracionService.obtenerProductos();
            log.info("CrmSmokeTestService | verificarConexionCrm | conexion con el CRM OK | productos={}",
                    productos.size());
        } catch (Exception e) {
            log.error("CrmSmokeTestService | verificarConexionCrm | no se pudo contactar al CRM: {}",
                    e.getMessage(), e);
        }
    }
}
