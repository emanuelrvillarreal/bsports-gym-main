package com.blue.open.Code.Event;

import com.blue.open.Code.Entity.DTO.AccesoClubRegistroDTO;
import org.springframework.context.ApplicationEvent;

public class AccesoClubRegistroEvent extends ApplicationEvent {
    private final AccesoClubRegistroDTO registro;
    private final Long companyId;

    public AccesoClubRegistroEvent(Object source, AccesoClubRegistroDTO registro, Long companyId) {
        super(source);
        this.registro = registro;
        this.companyId = companyId;
    }

    public AccesoClubRegistroDTO getRegistro() {
        return registro;
    }

    public Long getCompanyId() {
        return companyId;
    }
}
