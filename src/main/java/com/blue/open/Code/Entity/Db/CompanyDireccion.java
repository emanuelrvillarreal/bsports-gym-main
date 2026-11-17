package com.blue.open.Code.Entity.Db;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class CompanyDireccion {

    @Column(name = "direccion", length = 512)
    private String direccion;

    @Column(name = "codigo_postal", length = 16)
    private String codigoPostal;

    @Column(name = "localidad", length = 128)
    private String localidad;
}
