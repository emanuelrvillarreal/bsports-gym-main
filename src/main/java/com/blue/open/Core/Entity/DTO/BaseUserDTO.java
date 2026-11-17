package com.blue.open.Core.Entity.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class BaseUserDTO extends AbstractEntityDTO {
    private String username;
    private String password;
    private String email;
    private String phone;
    private List<UserCompanyRoleDTO> userCompanyRoles;
}
