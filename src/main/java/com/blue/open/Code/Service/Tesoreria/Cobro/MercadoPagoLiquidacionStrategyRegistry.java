package com.blue.open.Code.Service.Tesoreria.Cobro;

import com.blue.open.Code.Entity.Enums.MercadoPagoCobroTipoEnum;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class MercadoPagoLiquidacionStrategyRegistry {

    private final Map<MercadoPagoCobroTipoEnum, MercadoPagoLiquidacionStrategy> strategiesByTipo;

    public MercadoPagoLiquidacionStrategyRegistry(List<MercadoPagoLiquidacionStrategy> strategies) {
        this.strategiesByTipo = strategies.stream()
                .collect(Collectors.toMap(MercadoPagoLiquidacionStrategy::getTipo, Function.identity()));
    }

    public MercadoPagoLiquidacionStrategy get(MercadoPagoCobroTipoEnum tipo) {
        MercadoPagoLiquidacionStrategy strategy = strategiesByTipo.get(tipo);
        if (strategy == null) {
            throw new IllegalArgumentException("Tipo de liquidación MP no soportado: " + tipo);
        }
        return strategy;
    }
}
