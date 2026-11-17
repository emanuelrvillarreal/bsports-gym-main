package com.blue.open.Code.Service.EventoAcceso;

import com.blue.open.Code.Entity.Db.EventoAcceso;
import com.blue.open.Core.Exceptions.CustomException;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

public final class EventoAccesoCobroSupport {

    private EventoAccesoCobroSupport() {
    }

    public static String conceptoInvitadoFallback(EventoAcceso evento) {
        return "Acceso invitado - " + evento.getTitulo();
    }

    public static int resolveCantidadEntradas(Integer cantidadEntradas) {
        if (cantidadEntradas == null) {
            return 1;
        }
        if (cantidadEntradas < 1) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Cantidad de entradas debe ser al menos 1",
                    "EventoAccesoCobroSupport | cantidadEntradas: " + cantidadEntradas);
        }
        return cantidadEntradas;
    }

    public static BigDecimal calcularMontoTotal(BigDecimal precioUnitario, int cantidadEntradas) {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidadEntradas));
    }

    public static String sufijoCantidadEntradas(int cantidadEntradas) {
        return cantidadEntradas > 1 ? " (" + cantidadEntradas + " entradas)" : "";
    }
}
