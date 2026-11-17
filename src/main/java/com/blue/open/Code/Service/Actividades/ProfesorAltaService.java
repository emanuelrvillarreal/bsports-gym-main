package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Auth.RoleEnum;
import com.blue.open.Code.Entity.DTO.ProfesorAltaCommand;
import com.blue.open.Code.Entity.DTO.ProfesorDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Profesor;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.ProfesorEstadoEnum;
import com.blue.open.Code.Mapper.ProfesorMapper;
import com.blue.open.Code.Repository.ProfesorRepository;
import com.blue.open.Code.Service.MemberUserProvisioningService;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class ProfesorAltaService {

    private static final int STATUS_ACTIVE = 0;

    @Autowired
    private AuthService authService;

    @Autowired
    private MemberUserProvisioningService userProvisioningService;

    @Autowired
    private ProfesorRepository profesorRepository;

    @Autowired
    private ProfesorMapper profesorMapper;

    @Transactional
    public ProfesorDTO alta(ProfesorAltaCommand command) {
        authService.valid(List.of(PermissionEnum.ACTIVITY_CREATE));
        Company company = authService.getCurrentCompanyOrThrow();
        Profesor profesor = registrar(company, command);
        return profesorMapper.modelToDto(
                profesorRepository.findByIdWithUser(profesor.getId()).orElse(profesor));
    }

    private Profesor registrar(Company company, ProfesorAltaCommand command) {
        if (company == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Gimnasio requerido",
                    "ProfesorAltaService | company null");
        }
        if (command == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Datos requeridos",
                    "ProfesorAltaService | command null");
        }

        User user = userProvisioningService.resolveOrCreateMigrationUser(command, company, RoleEnum.PROFESOR);

        LocalDate fechaAlta = command.getFechaAlta() != null ? command.getFechaAlta() : LocalDate.now();

        // uk_profesores_company_user: puede existir como mucho una fila por club y usuario,
        // así que si fue dado de baja (baja lógica) se reactiva esa misma fila.
        Profesor existente = profesorRepository.findFirstByCompany_IdAndUser_Id(company.getId(), user.getId())
                .orElse(null);
        if (existente != null) {
            return reactivar(existente, fechaAlta);
        }

        Profesor profesor = new Profesor();
        profesor.setCompany(company);
        profesor.setUser(user);
        profesor.setEstado(ProfesorEstadoEnum.ACTIVO);
        profesor.setFechaAlta(fechaAlta);
        return profesorRepository.save(profesor);
    }

    private Profesor reactivar(Profesor profesor, LocalDate fechaAlta) {
        if (profesor.getStatus() == null || profesor.getStatus() == STATUS_ACTIVE) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Usuario ya registrado como profesor en el gimnasio",
                    "ProfesorAltaService | profesorId: " + profesor.getId());
        }

        profesor.setStatus(STATUS_ACTIVE);
        profesor.setEstado(ProfesorEstadoEnum.ACTIVO);
        profesor.setFechaAlta(fechaAlta);
        return profesorRepository.save(profesor);
    }
}
