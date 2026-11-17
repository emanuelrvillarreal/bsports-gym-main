package com.blue.open.Code.Service.Tesoreria;

import com.blue.open.Code.Entity.DTO.LiquidacionCuotaDetalle;
import com.blue.open.Code.Entity.Db.Cuota;
import com.blue.open.Code.Entity.Db.Movimiento;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.CuotaEstadoEnum;
import com.blue.open.Code.Entity.Enums.MovimientoEstadoEnum;
import com.blue.open.Code.Entity.Enums.MovimientoTipoEnum;
import com.blue.open.Code.Repository.CuotaRepository;
import com.blue.open.Code.Repository.MovimientoRepository;
import com.blue.open.Core.Exceptions.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class LiquidacionCuotaService {

    private static final int STATUS_ACTIVE = 0;

    @Autowired
    private CuotaRepository cuotaRepository;

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Transactional
    public Movimiento liquidarCuota(Cuota cuota, LiquidacionCuotaDetalle detalle, User registradoPor) {
        Cuota managed = cuotaRepository.findFirstByIdWithStatusIsActive(cuota.getId());
        if (managed == null) {
            throw new CustomException(HttpStatus.NOT_FOUND, "Cuota no encontrada",
                    "LiquidacionCuotaService | cuotaId: " + cuota.getId());
        }

        validarCuotaLiquidable(managed);
        validarFechaPago(managed, detalle);

        if (movimientoRepository.existsByCuotaIdAndEstadoAndStatus(
                managed.getId(), MovimientoEstadoEnum.ACTIVO, STATUS_ACTIVE)) {
            throw new CustomException(HttpStatus.CONFLICT, "Cuota ya tiene cobro registrado",
                    "LiquidacionCuotaService | cuotaId: " + managed.getId());
        }

        Movimiento movimiento = buildMovimiento(managed, detalle, registradoPor);
        managed.marcarPagada();

        movimientoRepository.save(movimiento);
        cuotaRepository.save(managed);

        return movimiento;
    }

    private void validarCuotaLiquidable(Cuota cuota) {
        CuotaEstadoEnum estado = cuota.getEstado();
        if (estado == CuotaEstadoEnum.PAGADA) {
            throw new CustomException(HttpStatus.CONFLICT, "Cuota ya pagada",
                    "LiquidacionCuotaService | cuotaUuid: " + cuota.getUuid());
        }
        if (estado == CuotaEstadoEnum.ANULADA) {
            throw new CustomException(HttpStatus.CONFLICT, "Cuota anulada",
                    "LiquidacionCuotaService | cuotaUuid: " + cuota.getUuid());
        }
        if (estado != CuotaEstadoEnum.PENDIENTE && estado != CuotaEstadoEnum.VENCIDA) {
            throw new CustomException(HttpStatus.CONFLICT, "Cuota no puede cobrarse en su estado actual",
                    "LiquidacionCuotaService | estado: " + estado);
        }
    }

    private void validarFechaPago(Cuota cuota, LiquidacionCuotaDetalle detalle) {
        LocalDate fechaPago = detalle.getFecha();
        LocalDate fechaGeneracion = cuota.getFechaGeneracion();
        if (fechaPago == null || fechaGeneracion == null) {
            return;
        }
        if (fechaPago.isBefore(fechaGeneracion)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Fecha de pago inválida",
                    "La fecha de pago no puede ser anterior a la fecha de generación de la cuota.");
        }
    }

    private Movimiento buildMovimiento(Cuota cuota, LiquidacionCuotaDetalle detalle, User registradoPor) {
        Movimiento movimiento = new Movimiento();
        movimiento.setCompany(cuota.getCompany());
        movimiento.setTipo(MovimientoTipoEnum.INGRESO);
        movimiento.setEstado(MovimientoEstadoEnum.ACTIVO);
        movimiento.setMonto(cuota.montoFinal());
        movimiento.setFecha(detalle.getFecha());
        movimiento.setMedioPago(detalle.getMedioPago());
        movimiento.setCuota(cuota);
        movimiento.setPersona(cuota.getPersona());
        movimiento.setTitular(cuota.getTitular());
        movimiento.setConcepto("Cobro " + cuota.getConcepto());
        movimiento.setRegistradoPor(registradoPor);
        movimiento.setObservaciones(detalle.getObservaciones());
        return movimiento;
    }
}
