package com.blue.open.Code.Service.Tesoreria.Cobro;

import com.blue.open.Code.Entity.DTO.LiquidacionCuotaDetalle;
import com.blue.open.Code.Entity.Db.Cuota;
import com.blue.open.Code.Entity.Db.MercadoPagoCobroIntent;
import com.blue.open.Code.Entity.Db.MercadoPagoCobroItem;
import com.blue.open.Code.Entity.Enums.MedioPagoEnum;
import com.blue.open.Code.Entity.Enums.MercadoPagoCobroTipoEnum;
import com.blue.open.Code.Service.Tesoreria.LiquidacionCuotaService;
import com.blue.open.Code.Service.Tesoreria.MemberEstadoRecalculoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class MercadoPagoLiquidacionCuotaStrategy implements MercadoPagoLiquidacionStrategy {

    private final LiquidacionCuotaService liquidacionCuotaService;
    private final MemberEstadoRecalculoService memberEstadoRecalculoService;

    @Override
    public MercadoPagoCobroTipoEnum getTipo() {
        return MercadoPagoCobroTipoEnum.CUOTA;
    }

    @Override
    public void liquidar(MercadoPagoCobroIntent intent) {
        LiquidacionCuotaDetalle detalle = new LiquidacionCuotaDetalle();
        detalle.setMedioPago(MedioPagoEnum.MERCADO_PAGO);
        detalle.setFecha(LocalDate.now());
        detalle.setObservaciones("Mercado Pago | paymentId: " + intent.getPaymentId());

        var mercadoPagoCobro = intent.getMercadoPagoCobro();
        Set<Long> titularIdsAfectados = new LinkedHashSet<>();
        for (MercadoPagoCobroItem item : mercadoPagoCobro.getItems()) {
            Cuota cuota = item.getCuota();
            liquidacionCuotaService.liquidarCuota(cuota, detalle, mercadoPagoCobro.getRegistradoPor());
            titularIdsAfectados.add(cuota.getTitular().getId());
        }

        if (!titularIdsAfectados.isEmpty()) {
            memberEstadoRecalculoService.recalcularGruposPorTitularIds(
                    intent.getCompany().getId(), titularIdsAfectados);
        }
    }
}
