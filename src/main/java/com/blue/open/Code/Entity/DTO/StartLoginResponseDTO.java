package com.blue.open.Code.Entity.DTO;

import lombok.Getter;

@Getter
public class StartLoginResponseDTO {

    private final String message;

    public StartLoginResponseDTO(String message) {
        this.message = message;
    }
}
