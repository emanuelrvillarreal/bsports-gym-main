package com.blue.open.Code.Entity.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PortalMemberGrupoDTO {

    private String titularMemberUuid;
    private List<PortalMemberCredencialDTO> integrantes = new ArrayList<>();
}
