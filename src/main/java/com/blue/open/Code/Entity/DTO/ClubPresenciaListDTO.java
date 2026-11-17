package com.blue.open.Code.Entity.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ClubPresenciaListDTO {

    private List<ClubPresenciaItemDTO> presentes;
    private int count;

    public ClubPresenciaListDTO(List<ClubPresenciaItemDTO> presentes) {
        this.presentes = presentes;
        this.count = presentes.size();
    }
}
