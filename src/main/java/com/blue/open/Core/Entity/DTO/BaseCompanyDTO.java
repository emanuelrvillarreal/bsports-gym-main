package com.blue.open.Core.Entity.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class BaseCompanyDTO extends AbstractEntityDTO {
    private String name;
    private String description;
    private String path;
    private String code;
}
