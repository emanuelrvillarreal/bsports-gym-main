package com.blue.open.Code.Service.support;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Service.CompanySuscripcionCrmService;
import com.blue.open.Crm.Entity.DTO.Response.CrmSuscripcionActivaDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmSuscripcionResponseDTO;

/**
 * Doble de prueba de la suscripcion del club al plan del CRM: registra con que datos se lo llamo,
 * sin tocar repositorios. Subclase real en vez de un mock de Mockito por el mismo motivo que
 * {@link RecordingCuotaSocialGeneracionService}: bytebuddy no puede instrumentar esta clase
 * concreta bajo JDK 25 en este entorno.
 */
public class RecordingCompanySuscripcionCrmService extends CompanySuscripcionCrmService {

    private boolean called;
    private Company lastCompany;
    private CrmSuscripcionActivaDTO lastSuscripcion;
    private RuntimeException exceptionToThrow;

    private boolean registrarCalled;
    private Company lastRegistrarCompany;
    private CrmSuscripcionResponseDTO lastSuscripcionResponse;

    private boolean registrarDesdeCrmCalled;
    private Company lastRegistrarDesdeCrmCompany;
    private CrmSuscripcionActivaDTO lastRegistrarDesdeCrmSuscripcion;

    private boolean anularCalled;
    private Company lastAnularCompany;

    @Override
    public void registrar(Company company, CrmSuscripcionResponseDTO suscripcionResponse) {
        this.registrarCalled = true;
        this.lastRegistrarCompany = company;
        this.lastSuscripcionResponse = suscripcionResponse;
    }

    @Override
    public void registrarDesdeCrm(Company company, CrmSuscripcionActivaDTO suscripcion) {
        this.registrarDesdeCrmCalled = true;
        this.lastRegistrarDesdeCrmCompany = company;
        this.lastRegistrarDesdeCrmSuscripcion = suscripcion;
    }

    public boolean wasRegistrarDesdeCrmCalled() {
        return registrarDesdeCrmCalled;
    }

    public Company getLastRegistrarDesdeCrmCompany() {
        return lastRegistrarDesdeCrmCompany;
    }

    public CrmSuscripcionActivaDTO getLastRegistrarDesdeCrmSuscripcion() {
        return lastRegistrarDesdeCrmSuscripcion;
    }

    @Override
    public void anular(Company company) {
        this.anularCalled = true;
        this.lastAnularCompany = company;
    }

    public boolean wasAnularCalled() {
        return anularCalled;
    }

    public Company getLastAnularCompany() {
        return lastAnularCompany;
    }

    public CrmSuscripcionResponseDTO getLastSuscripcionResponse() {
        return lastSuscripcionResponse;
    }

    @Override
    public void refrescar(Company company, CrmSuscripcionActivaDTO suscripcion) {
        this.called = true;
        this.lastCompany = company;
        this.lastSuscripcion = suscripcion;
        if (exceptionToThrow != null) {
            throw exceptionToThrow;
        }
    }

    public boolean wasRegistrarCalled() {
        return registrarCalled;
    }

    public Company getLastRegistrarCompany() {
        return lastRegistrarCompany;
    }

    public void setExceptionToThrow(RuntimeException exceptionToThrow) {
        this.exceptionToThrow = exceptionToThrow;
    }

    public boolean wasCalled() {
        return called;
    }

    public Company getLastCompany() {
        return lastCompany;
    }

    public CrmSuscripcionActivaDTO getLastSuscripcion() {
        return lastSuscripcion;
    }
}
