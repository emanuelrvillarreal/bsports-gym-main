package com.blue.open.Code.Entity.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CompanyContextDTO {
    /** true = multi-empresa (SaaS), false = mono-empresa (on-premise) */
    private boolean multiTenantEnabled;
    /** UUID de la empresa por defecto cuando multiTenantEnabled es false */
    private String defaultCompanyUuid;
    /** true si el frontend debe enviar X-Company-Uuid en cada request autenticado */
    private boolean companyHeaderRequired;
    /** Valor crudo del header X-Company-Uuid (puede ser null) */
    private String requestCompanyUuid;
    /** UUID resuelto tras aplicar resolveCompanyUuid (fallback a default si aplica) */
    private String resolvedCompanyUuid;
    /** Empresa activa de la sesión; null si no hay usuario autenticado o falta contexto */
    private CompanyDTO currentCompany;
}
