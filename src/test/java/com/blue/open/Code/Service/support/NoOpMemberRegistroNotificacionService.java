package com.blue.open.Code.Service.support;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Enums.MemberRegistroTipoEnum;
import com.blue.open.Code.Service.MemberRegistroNotificacionService;

public class NoOpMemberRegistroNotificacionService extends MemberRegistroNotificacionService {

    @Override
    public void notificarSolicitudPendiente(Company company, Member member, MemberRegistroTipoEnum tipoRegistro) {
        // no-op for unit tests
    }
}
