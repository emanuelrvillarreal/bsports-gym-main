package com.blue.open.Code.Service.Tesoreria.Cobro;

import com.blue.open.Code.Entity.DTO.CobroCuotaCommand;
import com.blue.open.Code.Entity.DTO.CobroCuotaResultDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.CanalCobroEnum;

public interface CanalCobroStrategy {

    CanalCobroEnum getCanal();

    CobroCuotaResultDTO procesar(CobroCuotaCommand command, Company company, User registradoPor);
}
