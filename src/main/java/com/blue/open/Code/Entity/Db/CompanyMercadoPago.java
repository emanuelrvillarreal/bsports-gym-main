package com.blue.open.Code.Entity.Db;

import com.blue.open.Core.Entity.Db.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Table(name = "company_mercado_pago")
@Entity
public class CompanyMercadoPago extends AbstractEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false, unique = true)
    private Company company;

    @Column(name = "habilitado", nullable = false, columnDefinition = "BIT")
    private Boolean habilitado = false;

    @Column(name = "access_token", length = 1024)
    private String accessToken;

    @Column(name = "public_key", length = 512)
    private String publicKey;

    @Column(name = "webhook_secret", length = 1024)
    private String webhookSecret;

    public boolean isWebhookOperativo() {
        return webhookSecret != null && !webhookSecret.isBlank();
    }

    public boolean isOperativo() {
        return Boolean.TRUE.equals(habilitado)
                && accessToken != null
                && !accessToken.isBlank();
    }

    public static CompanyMercadoPago crearDeshabilitadoVacias(Company company) {
        CompanyMercadoPago config = new CompanyMercadoPago();
        config.setCompany(company);
        config.setHabilitado(false);
        return config;
    }
}
