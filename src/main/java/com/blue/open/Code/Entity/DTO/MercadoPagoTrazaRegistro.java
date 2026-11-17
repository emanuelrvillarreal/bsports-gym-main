package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.MercadoPagoCobroIntent;
import com.blue.open.Code.Entity.Enums.MercadoPagoTrazaEtapaEnum;
import com.blue.open.Code.Entity.Enums.MercadoPagoTrazaNivelEnum;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MercadoPagoTrazaRegistro {

    private Company company;
    private MercadoPagoCobroIntent intent;
    private MercadoPagoTrazaEtapaEnum etapa;
    @Builder.Default
    private MercadoPagoTrazaNivelEnum nivel = MercadoPagoTrazaNivelEnum.INFO;
    private String externalReference;
    private String preferenceId;
    private Long paymentId;
    private String mensaje;
    private String requestRaw;
    private String responseRaw;
}
