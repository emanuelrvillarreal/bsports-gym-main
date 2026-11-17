package com.blue.open.Core.Entity.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordDTO {
    private String code;
    private String email;
    private String password;
}
