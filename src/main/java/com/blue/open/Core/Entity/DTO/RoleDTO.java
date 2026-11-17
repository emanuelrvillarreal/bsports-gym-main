package com.blue.open.Core.Entity.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RoleDTO extends AbstractEntityDTO {
    private String name;
    private Integer isDefault = 0;
    private List<PermissionDTO> permissions;
}
