package com.blue.open.Core.Service;

import com.blue.open.Core.Exceptions.CustomException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class CaptchaService {

    private final RestTemplate restTemplate;

    @Value("${captcha.secret}")
    private String secretKey;

    @Value("${captcha.enabled:true}")
    private boolean enabled;

    public CaptchaService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    public void verify(String token) {
        if (!enabled) {
            return;
        }

        if (token == null || token.isBlank()) {
            throw new CustomException("Captcha no completado");
        }

        String url = "https://hcaptcha.com/siteverify";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("secret", secretKey);
        map.add("response", token);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                new ParameterizedTypeReference<Map<String, Object>>() {}
            );
            Map<String, Object> body = response.getBody();

            if (body == null || body.get("success") == null || !(Boolean) body.get("success")) {
                throw new CustomException("Verificación de Captcha fallida");
            }
        } catch (Exception e) {
            throw new CustomException("Error al verificar el captcha: " + e.getMessage());
        }
    }
}
