package com.blue.open.Core.Entity.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PublicRegisterRequest<UDTO extends BaseUserDTO, CDTO extends BaseCompanyDTO> {
    private UDTO user;
    private CDTO company;
}
