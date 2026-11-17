package com.blue.open.Code.Entity.DTO;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ConfigurationDTO {
    private String name, value, companyUuid;
    private boolean global;
}
