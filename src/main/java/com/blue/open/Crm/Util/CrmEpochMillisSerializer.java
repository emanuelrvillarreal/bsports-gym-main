package com.blue.open.Crm.Util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Serializador Jackson que convierte un LocalDateTime a epoch millis (numero).
 * <p>
 * Motivo: los campos fecha del CRM (blue-crm) son java.sql.Timestamp sin @JsonFormat,
 * por lo que viajan por la red como numero de milisegundos desde epoch. Sin este
 * serializador, Jackson enviaria un array o un string ISO y el CRM no lo podria leer.
 * <p>
 * Zona horaria: America/Argentina/Buenos_Aires, coherente con spring.jackson.time-zone
 * y con el TimeZone.setDefault() de Application.
 * <p>
 * Espera: un LocalDateTime (puede ser null).
 * Responde: escribe un numero long en el JSON, o null si el valor es null.
 */
public class CrmEpochMillisSerializer extends JsonSerializer<LocalDateTime> {

    private static final ZoneId ZONA_ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");

    @Override
    public void serialize(LocalDateTime valor, JsonGenerator generator, SerializerProvider provider)
            throws IOException {
        if (valor == null) {
            generator.writeNull();
            return;
        }
        generator.writeNumber(valor.atZone(ZONA_ARGENTINA).toInstant().toEpochMilli());
    }
}
