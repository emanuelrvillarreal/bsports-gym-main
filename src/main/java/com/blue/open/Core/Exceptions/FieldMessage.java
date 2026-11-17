package com.blue.open.Core.Exceptions;

import lombok.ToString;

@ToString 
public class FieldMessage {
    private String nombreCampo;
    private String message;

    public FieldMessage() {
    }

    public FieldMessage(String nombreCampo, String message) {
        this.nombreCampo = nombreCampo;
        this.message = message;
    }

    public String getNombreCampo() {
        return nombreCampo;
    }

    public void setNombreCampo(String nombreCampo) {
        this.nombreCampo = nombreCampo;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
