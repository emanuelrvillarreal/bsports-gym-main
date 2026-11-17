package com.blue.open.Core.Entity.DTO;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class JwtTokenDTO {
    private String token;
    private String refresh;
}
