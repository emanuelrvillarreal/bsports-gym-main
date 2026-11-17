package com.blue.open.Code.Entity.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SysadminMercadoPagoConfigResponseDTO {

    private String companyUuid;
    private Boolean habilitado;
    private boolean operativo;
    private boolean webhookOperativo;
    private boolean accessTokenConfigured;
    private String accessTokenHint;
    private String publicKey;
    private boolean webhookSecretConfigured;
    private String webhookSecretHint;
}
