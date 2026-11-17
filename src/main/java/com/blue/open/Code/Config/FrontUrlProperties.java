package com.blue.open.Code.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;

/**
 * URLs del frontend. {@code front.url} es la base; las propiedades específicas
 * vacías heredan ese valor.
 */
@Component
public class FrontUrlProperties {

    @Value("${front.url}")
    private String url;

    /** Enlaces en correos de registro y recuperacion de contrasena. */
    @Value("${front.url.mail:}")
    private String mailUrl;

    /** Orígenes CORS del endpoint WebSocket (lista separada por comas). */
    @Value("${front.url.websocket:}")
    private String websocketUrls;

    public String getUrl() {
        return url != null ? url.trim() : "";
    }

    public String getMailUrl() {
        return resolve(mailUrl);
    }

    public String[] getWebsocketAllowedOrigins() {
        return parseCommaSeparated(resolve(websocketUrls));
    }

    private String resolve(String specific) {
        if (StringUtils.hasText(specific)) {
            return specific.trim();
        }
        return getUrl();
    }

    private static String[] parseCommaSeparated(String raw) {
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toArray(String[]::new);
    }
}
