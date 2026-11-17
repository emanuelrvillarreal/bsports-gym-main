package com.blue.open.Crm.Util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

/**
 * Deserializador Jackson que convierte una fecha recibida del CRM (blue-crm) a LocalDateTime.
 * <p>
 * Motivo: los campos fecha del CRM son java.sql.Timestamp sin @JsonFormat, por lo que llegan
 * como numero de milisegundos desde epoch. Sin este deserializador, Jackson fallaria al
 * mapearlos a LocalDateTime.
 * <p>
 * Tolera tres formas para no romper si el CRM cambia el formato:
 * numero (epoch millis), string numerico (epoch millis) y string ISO-8601.
 * <p>
 * Zona horaria: America/Argentina/Buenos_Aires, coherente con spring.jackson.time-zone
 * y con el TimeZone.setDefault() de Application.
 * <p>
 * Espera: un token JSON numerico, string o null.
 * Responde: un LocalDateTime, o null si el valor es null / vacio / no interpretable.
 */
public class CrmEpochMillisDeserializer extends JsonDeserializer<LocalDateTime> {

    private static final ZoneId ZONA_ARGENTINA = ZoneId.of("America/Argentina/Buenos_Aires");

    @Override
    public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        if (parser.currentToken() == JsonToken.VALUE_NULL) {
            return null;
        }

        if (parser.currentToken() == JsonToken.VALUE_NUMBER_INT) {
            return this.desdeEpochMillis(parser.getLongValue());
        }

        String texto = parser.getValueAsString();
        if (texto == null || texto.isBlank()) {
            return null;
        }
        texto = texto.trim();

        // El CRM podria pasar a mandar la fecha como string; se contemplan ambos casos
        try {
            return this.desdeEpochMillis(Long.parseLong(texto));
        } catch (NumberFormatException e) {
            try {
                return LocalDateTime.parse(texto);
            } catch (DateTimeParseException ex) {
                return null;
            }
        }
    }

    // Convierte milisegundos desde epoch a LocalDateTime en horario argentino
    private LocalDateTime desdeEpochMillis(long millis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZONA_ARGENTINA);
    }
}
