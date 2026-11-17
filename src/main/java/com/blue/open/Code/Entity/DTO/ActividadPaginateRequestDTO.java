package com.blue.open.Code.Entity.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActividadPaginateRequestDTO {

    private int page = 1;
    private int size = 10;
    private String search;
    private Boolean completa;
    private String profesorUuid;
}
