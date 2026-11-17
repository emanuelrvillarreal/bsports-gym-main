package com.blue.open.Code.Entity.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Solapas del menu administrativo que el club decidio ocultar. Lo usan GET y PUT
 * /companies/menu-solapas: el front arma el menu y las secciones con solapas a partir de
 * esta lista, para el ADMIN y para ADM_RECEPCION del mismo club.
 *
 * Cada elemento es el identificador de una solapa definido por el front (ej: "ingresos",
 * "solicitudes"). El backend no conoce el catalogo: solo valida el formato.
 */
@Getter
@Setter
public class MenuSolapasDTO {

    @NotNull(message = "Debe informar la lista de solapas ocultas (puede ser vacía)")
    @Size(max = 50, message = "No se pueden ocultar más de 50 solapas")
    private List<
            @NotBlank(message = "El identificador de la solapa no puede estar vacío")
            @Pattern(regexp = "^[a-z0-9-]{1,60}$",
                    message = "El identificador de la solapa solo admite minúsculas, números y guiones")
            String> solapasOcultas = new ArrayList<>();
}
