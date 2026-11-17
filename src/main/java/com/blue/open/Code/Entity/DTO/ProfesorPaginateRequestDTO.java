package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.ProfesorEstadoEnum;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ProfesorPaginateRequestDTO {

    private int page = 1;
    private int size = 10;
    private String search;
    private List<ProfesorEstadoEnum> estados = new ArrayList<>();
}
