package com.blue.open.Code.Entity.DTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class MercadoPagoWebhookNotificationDTO {

    private String action;
    private String type;

    @JsonProperty("api_version")
    private String apiVersion;

    @JsonProperty("date_created")
    private String dateCreated;

    private String id;

    @JsonProperty("live_mode")
    private Boolean liveMode;

    @JsonProperty("user_id")
    private Long userId;

    private WebhookData data;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WebhookData {
        private String id;
    }
}
