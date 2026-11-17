package com.blue.open.Code.Entity.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AccesoClubPresenciaUpdateDTO {
    private AccesoClubItemDTO item;
    private int presenteCount;
}
