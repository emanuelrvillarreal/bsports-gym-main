package com.blue.open.Code.Service.support;

import com.blue.open.Code.Entity.DTO.ClubRegistrationRequestDTO;
import com.blue.open.Code.Service.ClubCrmSincronizacionService;
import com.blue.open.Crm.Entity.DTO.Request.CrmDescuentoRequestDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmClienteResponseDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmSuscripcionResponseDTO;

/**
 * Doble de prueba del alta del club en el CRM: registra con que request se lo llamo y devuelve una
 * respuesta preparada, sin salir a la red. Subclase real en vez de un mock de Mockito por el mismo
 * motivo que {@link RecordingCuotaSocialGeneracionService}: bytebuddy no puede instrumentar esta
 * clase concreta bajo JDK 25 en este entorno.
 */
public class RecordingClubCrmSincronizacionService extends ClubCrmSincronizacionService {

    private boolean registrarClienteCalled;
    private ClubRegistrationRequestDTO lastRegistrarClienteRequest;
    private boolean suscribirCalled;
    private String lastSuscribirCuit;
    private String lastSuscribirCodigoPlan;
    private CrmDescuentoRequestDTO lastSuscribirDescuento;
    private CrmSuscripcionResponseDTO respuesta = new CrmSuscripcionResponseDTO();
    private RuntimeException exceptionToThrow;
    private RuntimeException suscribirExceptionToThrow;

    // El CrmIntegracionService real nunca se usa: los metodos de alta estan overrideados
    public RecordingClubCrmSincronizacionService() {
        super(null);
    }

    @Override
    public CrmClienteResponseDTO registrarCliente(ClubRegistrationRequestDTO request) {
        this.registrarClienteCalled = true;
        this.lastRegistrarClienteRequest = request;
        if (exceptionToThrow != null) {
            throw exceptionToThrow;
        }
        return new CrmClienteResponseDTO();
    }

    @Override
    public CrmSuscripcionResponseDTO suscribir(String cuit, String codigoPlan, CrmDescuentoRequestDTO descuento) {
        this.suscribirCalled = true;
        this.lastSuscribirCuit = cuit;
        this.lastSuscribirCodigoPlan = codigoPlan;
        this.lastSuscribirDescuento = descuento;
        if (suscribirExceptionToThrow != null) {
            throw suscribirExceptionToThrow;
        }
        return respuesta;
    }

    public void setRespuesta(CrmSuscripcionResponseDTO respuesta) {
        this.respuesta = respuesta;
    }

    public void setSuscribirExceptionToThrow(RuntimeException suscribirExceptionToThrow) {
        this.suscribirExceptionToThrow = suscribirExceptionToThrow;
    }

    public boolean wasSuscribirCalled() {
        return suscribirCalled;
    }

    public String getLastSuscribirCuit() {
        return lastSuscribirCuit;
    }

    public String getLastSuscribirCodigoPlan() {
        return lastSuscribirCodigoPlan;
    }

    public CrmDescuentoRequestDTO getLastSuscribirDescuento() {
        return lastSuscribirDescuento;
    }

    public void setExceptionToThrow(RuntimeException exceptionToThrow) {
        this.exceptionToThrow = exceptionToThrow;
    }

    public boolean wasRegistrarClienteCalled() {
        return registrarClienteCalled;
    }

    public ClubRegistrationRequestDTO getLastRegistrarClienteRequest() {
        return lastRegistrarClienteRequest;
    }

    public CrmSuscripcionResponseDTO getRespuesta() {
        return respuesta;
    }
}
