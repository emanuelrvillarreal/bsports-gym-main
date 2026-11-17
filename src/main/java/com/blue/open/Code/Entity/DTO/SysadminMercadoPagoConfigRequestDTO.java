package com.blue.open.Code.Entity.DTO;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SysadminMercadoPagoConfigRequestDTO {

    @Size(max = 512, message = "Access token demasiado largo")
    private String accessToken;

    @Size(max = 512, message = "Public key demasiado larga")
    private String publicKey;

    @Size(max = 512, message = "Webhook secret demasiado largo")
    private String webhookSecret;

    private Boolean habilitado;

    @AssertTrue(message = "El access token no puede estar vacío")
    public boolean isAccessTokenValid() {
        return accessToken == null || !accessToken.isBlank();
    }

    @AssertTrue(message = "El webhook secret no puede estar vacío")
    public boolean isWebhookSecretValid() {
        return webhookSecret == null || !webhookSecret.isBlank();
    }
}
