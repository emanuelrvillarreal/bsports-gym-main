package com.blue.open.Code.Service.support;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Service.MemberCupoService;
import com.blue.open.Core.Exceptions.CustomException;

/**
 * Doble de prueba que registra los argumentos con los que se pide validar el cupo de socios, sin
 * tocar repositorios. Subclase real en vez de un mock de Mockito por el mismo motivo que
 * {@link RecordingCuotaSocialGeneracionService}: bytebuddy no puede instrumentar esta clase
 * concreta bajo JDK 25 en este entorno.
 */
public class RecordingMemberCupoService extends MemberCupoService {

    private boolean called;
    private Company lastCompany;
    private Integer lastNuevosSocios;
    private CustomException exceptionToThrow;

    @Override
    public void validarCupoDisponible(Company company, int nuevosSocios) {
        this.called = true;
        this.lastCompany = company;
        this.lastNuevosSocios = nuevosSocios;
        if (exceptionToThrow != null) {
            throw exceptionToThrow;
        }
    }

    public void setExceptionToThrow(CustomException exceptionToThrow) {
        this.exceptionToThrow = exceptionToThrow;
    }

    public boolean wasCalled() {
        return called;
    }

    public Company getLastCompany() {
        return lastCompany;
    }

    public Integer getLastNuevosSocios() {
        return lastNuevosSocios;
    }
}
