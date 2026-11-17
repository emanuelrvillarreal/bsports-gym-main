package com.blue.open.Code.Entity.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MemberDescuentosDTO {

    private String memberUuid;
    private String nombre;
    private String fotoUrl;
    private List<DescuentoAsignadoDTO> cuotaSocial;
    private List<MemberDescuentoActividadDTO> actividades;
}
