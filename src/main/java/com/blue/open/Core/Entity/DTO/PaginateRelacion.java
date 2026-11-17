package com.blue.open.Core.Entity.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaginateRelacion {
    private String tipo; // inner o left
    private String hastaTabla;
    private String hastaColumna;
    private String desdeTabla;
    private String desdeColumna;
}
