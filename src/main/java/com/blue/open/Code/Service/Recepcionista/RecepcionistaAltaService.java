package com.blue.open.Code.Service.Recepcionista;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Auth.RoleEnum;
import com.blue.open.Code.Entity.DTO.RecepcionistaAltaCommand;
import com.blue.open.Code.Entity.DTO.RecepcionistaDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Recepcionista;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.RecepcionistaEstadoEnum;
import com.blue.open.Code.Mapper.RecepcionistaMapper;
import com.blue.open.Code.Repository.RecepcionistaRepository;
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
public class RecepcionistaAltaService {

    private static final int STATUS_ACTIVE = 0;

    @Autowired
    private AuthService authService;

    @Autowired
    private MemberUserProvisioningService userProvisioningService;

    @Autowired
    private RecepcionistaRepository recepcionistaRepository;

    @Autowired
    private RecepcionistaMapper recepcionistaMapper;

    @Transactional
    public RecepcionistaDTO alta(RecepcionistaAltaCommand command) {
        authService.valid(List.of(PermissionEnum.CLUB_INTERNAL_USER_MANAGE));
        Company company = authService.getCurrentCompanyOrThrow();
        Recepcionista recepcionista = registrar(company, command);
        return recepcionistaMapper.modelToDto(
                recepcionistaRepository.findByIdWithUser(recepcionista.getId()).orElse(recepcionista));
    }

    private Recepcionista registrar(Company company, RecepcionistaAltaCommand command) {
        if (company == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Gimnasio requerido",
                    "RecepcionistaAltaService | company null");
        }
        if (command == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Datos requeridos",
                    "RecepcionistaAltaService | command null");
        }

        User user = userProvisioningService.resolveOrCreateMigrationUser(command, company, RoleEnum.ADM_RECEPCION);

        LocalDate fechaAlta = command.getFechaAlta() != null ? command.getFechaAlta() : LocalDate.now();

        // uk_recepcionistas_company_user: puede existir como mucho una fila por club y usuario,
        // así que si fue dado de baja (baja lógica) se reactiva esa misma fila.
        Recepcionista existente = recepcionistaRepository.findFirstByCompany_IdAndUser_Id(company.getId(), user.getId())
                .orElse(null);
        if (existente != null) {
            return reactivar(existente, fechaAlta);
        }

        Recepcionista recepcionista = new Recepcionista();
        recepcionista.setCompany(company);
        recepcionista.setUser(user);
        recepcionista.setEstado(RecepcionistaEstadoEnum.ACTIVO);
        recepcionista.setFechaAlta(fechaAlta);
        return recepcionistaRepository.save(recepcionista);
    }

    private Recepcionista reactivar(Recepcionista recepcionista, LocalDate fechaAlta) {
        if (recepcionista.getStatus() == null || recepcionista.getStatus() == STATUS_ACTIVE) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Usuario ya registrado como recepcionista en el gimnasio",
                    "RecepcionistaAltaService | recepcionistaId: " + recepcionista.getId());
        }

        recepcionista.setStatus(STATUS_ACTIVE);
        recepcionista.setEstado(RecepcionistaEstadoEnum.ACTIVO);
        recepcionista.setFechaAlta(fechaAlta);
        return recepcionistaRepository.save(recepcionista);
    }
}
