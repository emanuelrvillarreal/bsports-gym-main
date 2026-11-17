package com.blue.open.Code.Service.Amenities;

import com.blue.open.Code.Entity.Db.Reserva;
import com.blue.open.Code.Entity.Enums.ReservaAmenityCobroTipoEnum;

public final class ReservaAmenityCobroSupport {

    private ReservaAmenityCobroSupport() {
    }

    public static String concepto(Reserva reserva, ReservaAmenityCobroTipoEnum tipoCobro) {
        String amenityNombre = reserva.getAmenity() != null ? reserva.getAmenity().getNombre() : "Amenity";
        String prefijo = switch (tipoCobro) {
            case SENA -> "Seña reserva amenity";
            case SALDO -> "Saldo reserva amenity";
            case TOTAL -> "Pago total reserva amenity";
        };
        return prefijo + " - " + amenityNombre;
    }
}
