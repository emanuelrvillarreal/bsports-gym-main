package com.blue.open.Code.Service.Tesoreria.Cobro;

import com.blue.open.Code.Entity.Db.MercadoPagoCobroIntent;
import com.blue.open.Code.Entity.Enums.MercadoPagoCobroTipoEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MercadoPagoLiquidacionOtroStrategy implements MercadoPagoLiquidacionStrategy {

    @Override
    public MercadoPagoCobroTipoEnum getTipo() {
        return MercadoPagoCobroTipoEnum.OTRO;
    }

    @Override
    public void liquidar(MercadoPagoCobroIntent intent) {
        var mercadoPagoCobro = intent.getMercadoPagoCobro();
        log.info("MP liquidación OTRO | intentUuid={} | paymentId={} | monto={}",
                intent.getUuid(),
                intent.getPaymentId(),
                mercadoPagoCobro.getMontoTotal());
    }
}
