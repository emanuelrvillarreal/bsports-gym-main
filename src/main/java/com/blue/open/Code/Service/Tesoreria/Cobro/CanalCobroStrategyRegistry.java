package com.blue.open.Code.Service.Tesoreria.Cobro;

import com.blue.open.Code.Entity.Enums.CanalCobroEnum;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CanalCobroStrategyRegistry {

    private final Map<CanalCobroEnum, CanalCobroStrategy> strategiesByCanal;

    public CanalCobroStrategyRegistry(List<CanalCobroStrategy> strategies) {
        this.strategiesByCanal = strategies.stream()
                .collect(Collectors.toMap(CanalCobroStrategy::getCanal, Function.identity()));
    }

    public CanalCobroStrategy get(CanalCobroEnum canal) {
        CanalCobroStrategy strategy = strategiesByCanal.get(canal);
        if (strategy == null) {
            throw new IllegalArgumentException("Canal de cobro no soportado: " + canal);
        }
        return strategy;
    }
}
