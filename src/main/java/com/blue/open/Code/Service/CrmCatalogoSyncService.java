package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.Db.PlanCrm;
import com.blue.open.Code.Entity.Db.ProductoCrm;
import com.blue.open.Code.Repository.PlanCrmRepository;
import com.blue.open.Code.Repository.ProductoCrmRepository;
import com.blue.open.Crm.Entity.DTO.Response.CrmPeriodicidadDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmPlanResponseDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmProductoResponseDTO;
import com.blue.open.Crm.Service.CrmIntegracionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Sincroniza el catalogo de planes y productos del CRM (blue-crm) contra las tablas locales
 * plan_crm y productos_crm, para tenerlo disponible sin depender de una llamada en vivo al CRM
 * (esos endpoints ya estan cacheados del lado del CRM, pero igual conviene una copia local).
 * <p>
 * Lo dispara CrmCatalogoSyncScheduler para el catalogo completo. CrmWebhookProcesador tambien
 * usa el upsert de un solo plan/producto (actualizarPlan/actualizarProducto) al recibir
 * PLAN_CAMBIO/PRODUCTO_CAMBIO, para no esperar a la proxima sincronizacion diaria.
 * <p>
 * Upsert por codigo: no da de baja los que dejaron de venir en la respuesta del CRM.
 */
@Service
@Slf4j
public class CrmCatalogoSyncService {

    private static final int STATUS_ACTIVE = 0;

    @Autowired
    private CrmIntegracionService crmIntegracionService;

    @Autowired
    private PlanCrmRepository planCrmRepository;

    @Autowired
    private ProductoCrmRepository productoCrmRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public void sincronizar() {
        this.sincronizarPlanes();
        this.sincronizarProductos();
    }

    private void sincronizarPlanes() {
        List<CrmPlanResponseDTO> planes = this.crmIntegracionService.obtenerPlanes();
        for (CrmPlanResponseDTO dto : planes) {
            this.actualizarPlan(dto);
        }
        log.info("CrmCatalogoSyncService | sincronizarPlanes | sincronizados={}", planes.size());
    }

    private void sincronizarProductos() {
        List<CrmProductoResponseDTO> productos = this.crmIntegracionService.obtenerProductos();
        for (CrmProductoResponseDTO dto : productos) {
            this.actualizarProducto(dto);
        }
        log.info("CrmCatalogoSyncService | sincronizarProductos | sincronizados={}", productos.size());
    }

    /**
     * Upsert de un solo plan por codigo. Lo usa tambien CrmWebhookProcesador al recibir
     * PLAN_CAMBIO, con el mismo payload que devuelve obtenerPlanes.
     * <p>
     * Si el plan no trae codigo, se ignora sin lanzar: no hay con que identificarlo para el
     * upsert.
     */
    @Transactional
    public void actualizarPlan(CrmPlanResponseDTO dto) {
        if (dto == null || dto.getCodigo() == null || dto.getCodigo().isBlank()) {
            return;
        }
        PlanCrm plan = this.planCrmRepository.findFirstByCodigoAndStatus(dto.getCodigo(), STATUS_ACTIVE)
                .orElseGet(() -> PlanCrm.crear(dto.getCodigo()));
        plan.actualizar(
                dto.getNombre(),
                dto.getDescripcion(),
                dto.getPrecio(),
                dto.getPrecioEsFinal(),
                dto.getAlicuotaIva(),
                this.periodicidadCodigo(dto.getPeriodicidad()),
                this.serializar(dto.getProductos(), dto.getCodigo()));
        this.planCrmRepository.save(plan);
    }

    /**
     * Upsert de un solo producto por codigo. Lo usa tambien CrmWebhookProcesador al recibir
     * PRODUCTO_CAMBIO, con el mismo payload que devuelve obtenerProductos.
     */
    @Transactional
    public void actualizarProducto(CrmProductoResponseDTO dto) {
        if (dto == null || dto.getCodigo() == null || dto.getCodigo().isBlank()) {
            return;
        }
        ProductoCrm producto = this.productoCrmRepository.findFirstByCodigoAndStatus(dto.getCodigo(), STATUS_ACTIVE)
                .orElseGet(() -> ProductoCrm.crear(dto.getCodigo()));
        producto.actualizar(dto.getNombre(), dto.getDescripcion(), dto.getPrecio(), dto.getAlicuotaIva(), dto.getUnidad());
        this.productoCrmRepository.save(producto);
    }

    private String periodicidadCodigo(CrmPeriodicidadDTO periodicidad) {
        return periodicidad != null ? periodicidad.getCodigo() : null;
    }

    private String serializar(Object valor, String codigoPlan) {
        if (valor == null) {
            return null;
        }
        try {
            return this.objectMapper.writeValueAsString(valor);
        } catch (JsonProcessingException e) {
            log.warn("CrmCatalogoSyncService | serializar | no se pudo serializar productos del plan | codigoPlan={}",
                    codigoPlan, e);
            return null;
        }
    }
}
