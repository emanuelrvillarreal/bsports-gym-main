package com.blue.open.Code.Service.Tesoreria;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.MovimientoDetalleDTO;
import com.blue.open.Code.Entity.DTO.MovimientoManualCommand;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Movimiento;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.MovimientoEstadoEnum;
import com.blue.open.Code.Entity.Enums.MovimientoTipoEnum;
import com.blue.open.Code.Mapper.MovimientoMapper;
import com.blue.open.Code.Repository.MovimientoRepository;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class MovimientoManualService {

    @Autowired
    private AuthService authService;

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Autowired
    private MovimientoMapper movimientoMapper;

    @Transactional
    public MovimientoDetalleDTO registrar(MovimientoManualCommand command) {
        MovimientoTipoEnum tipo = command.getTipo();
        PermissionEnum permiso = tipo == MovimientoTipoEnum.INGRESO
                ? PermissionEnum.TREASURY_COLLECT
                : PermissionEnum.TREASURY_EXPENSE;
        User registradoPor = authService.valid(List.of(permiso));
        Company company = authService.getCurrentCompanyOrThrow();

        validarCommand(command, tipo);

        Movimiento movimiento = new Movimiento();
        movimiento.setCompany(company);
        movimiento.setTipo(tipo);
        movimiento.setEstado(MovimientoEstadoEnum.ACTIVO);
        movimiento.setMonto(command.getMonto());
        movimiento.setFecha(command.getFecha() != null ? command.getFecha() : LocalDate.now());
        movimiento.setConcepto(command.getConcepto().trim());
        movimiento.setMedioPago(command.getMedioPago());
        movimiento.setObservaciones(command.getObservaciones());
        movimiento.setRegistradoPor(registradoPor);

        movimientoRepository.save(movimiento);
        return movimientoMapper.toDetalle(movimiento);
    }

    private void validarCommand(MovimientoManualCommand command, MovimientoTipoEnum tipo) {
        if (tipo == MovimientoTipoEnum.INGRESO && command.getMedioPago() == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Medio de pago requerido para ingresos",
                    "MovimientoManualService | medioPago null");
        }
        if (tipo == MovimientoTipoEnum.EGRESO && command.getMedioPago() != null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Los egresos no llevan medio de pago",
                    "MovimientoManualService | medioPago en egreso");
        }
    }
}
