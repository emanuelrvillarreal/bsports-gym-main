package com.blue.open.Code.Service.EventoAcceso.Cobro;

import com.blue.open.Code.Entity.DTO.EventoAccesoCobroResultDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.EventoAcceso;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.EventoAccesoHistorialTipoEnum;

public interface EventoAccesoCobroStrategy {

    EventoAccesoHistorialTipoEnum getTipo();

    EventoAccesoCobroResultDTO cobrar(Company company, User registradoPor, EventoAcceso evento, Object payload);
}
