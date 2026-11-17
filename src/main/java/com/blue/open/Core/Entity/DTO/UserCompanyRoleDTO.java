package com.blue.open.Core.Entity.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserCompanyRoleDTO extends AbstractEntityDTO {
    private String companyUuid;
    private String companyName;
    private String companyCuit;
    private String logoUrl;
    private List<RoleDTO> roles;
    // Si el club tiene bloqueado el acceso a la plataforma por facturacion vencida
    private Boolean companyBlocked;
}
