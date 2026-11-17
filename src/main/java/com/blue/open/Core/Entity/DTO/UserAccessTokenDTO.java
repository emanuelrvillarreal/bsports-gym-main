package com.blue.open.Core.Entity.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserAccessTokenDTO extends AbstractEntityDTO {
    private String userUuid;
    @NotBlank(message = "El nombre es obligatorio")
    private String name;
    private String code;
    private int active;
}
