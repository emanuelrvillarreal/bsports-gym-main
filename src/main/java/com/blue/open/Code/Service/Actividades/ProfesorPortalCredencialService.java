package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.PortalProfesorCredencialDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Profesor;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Mapper.ProfesorMapper;
import com.blue.open.Code.Service.ProfesorAuthService;
import com.blue.open.Code.Service.QrCodeService;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProfesorPortalCredencialService {

    @Autowired
    private AuthService authService;

    @Autowired
    private ProfesorAuthService profesorAuthService;

    @Autowired
    private ProfesorMapper profesorMapper;

    @Autowired
    private QrCodeService qrCodeService;

    @Transactional(readOnly = true)
    public PortalProfesorCredencialDTO obtenerCredencial() {
        authService.valid(List.of(PermissionEnum.PORTAL_CARD_VIEW));
        Company company = authService.getCurrentCompanyOrThrow();
        Profesor profesor = profesorAuthService.requirePortalEligible(
                profesorAuthService.getCurrentProfesorOrThrow(company.getId()));
        return toCredencial(profesor);
    }

    private PortalProfesorCredencialDTO toCredencial(Profesor profesor) {
        User user = profesor.getUser();
        PortalProfesorCredencialDTO dto = new PortalProfesorCredencialDTO();
        dto.setProfesor(profesorMapper.modelToDto(profesor));
        if (user != null) {
            dto.setUserUuid(user.getUuid());
            dto.setQr(qrCodeService.generateBase64Png(user.getUuid()));
        }
        return dto;
    }
}
