package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.CompanySuscripcionCrm;
import com.blue.open.Code.Entity.Db.PlanCrm;
import com.blue.open.Code.Repository.CompanySuscripcionCrmRepository;
import com.blue.open.Crm.Entity.DTO.Response.CrmPlanProductoDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Resuelve el limite ("unidades") de un producto puntual dentro de plan.productos[]
 * (PlanCrm.productosJson) para la suscripcion activa de un club. Usado por los distintos
 * chequeos de cupo (socios, administradores) que comparten el mismo mecanismo pero contra
 * productos CRM distintos.
 */
@Component
@Slf4j
public class CrmPlanLimiteResolver {

    private static final int STATUS_ACTIVE_SUSCRIPCION = 0;

    @Autowired
    private CompanySuscripcionCrmRepository companySuscripcionCrmRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Optional<Integer> resolverLimite(Company company, String codigoProducto) {
        return this.companySuscripcionCrmRepository
                .findFirstByCompanyIdWithPlan(company.getId(), STATUS_ACTIVE_SUSCRIPCION)
                .map(CompanySuscripcionCrm::getPlan)
                .map(PlanCrm::getProductosJson)
                .flatMap(productosJson -> this.extraerLimite(productosJson, codigoProducto));
    }

    private Optional<Integer> extraerLimite(String productosJson, String codigoProducto) {
        if (productosJson == null || productosJson.isBlank()) {
            return Optional.empty();
        }
        try {
            List<CrmPlanProductoDTO> productos = this.objectMapper.readValue(
                    productosJson, new TypeReference<List<CrmPlanProductoDTO>>() { });
            return productos.stream()
                    .filter(p -> p.getCodigo() != null
                            && p.getCodigo().trim().equalsIgnoreCase(codigoProducto))
                    .map(CrmPlanProductoDTO::getUnidades)
                    .findFirst();
        } catch (JsonProcessingException e) {
            log.warn("CrmPlanLimiteResolver | extraerLimite | productosJson invalido", e);
            return Optional.empty();
        }
    }
}
