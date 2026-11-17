package com.blue.open.Code.Entity.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PortalMemberCredencialDTO {

    private String userUuid;
    private String qr;
    private MemberDTO member;
}
