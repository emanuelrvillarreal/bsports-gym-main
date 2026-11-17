package com.blue.open.Code.Service.Tesoreria.Cobro;

import com.blue.open.Code.Entity.Db.MercadoPagoCobroIntent;
import com.blue.open.Code.Entity.Enums.MercadoPagoCobroTipoEnum;

public interface MercadoPagoLiquidacionStrategy {

    MercadoPagoCobroTipoEnum getTipo();

    void liquidar(MercadoPagoCobroIntent intent);
}
