package com.blue.open.Code.Service.Tesoreria.Cobro;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.MercadoPagoCobro;
import com.blue.open.Code.Entity.Db.MercadoPagoCobroIntent;
import com.blue.open.Code.Entity.Db.Movimiento;
import com.blue.open.Code.Entity.Db.Reserva;
import com.blue.open.Code.Entity.Db.Slot;
import com.blue.open.Code.Entity.Enums.MedioPagoEnum;
import com.blue.open.Code.Entity.Enums.MercadoPagoCobroTipoEnum;
import com.blue.open.Code.Entity.Enums.MovimientoEstadoEnum;
import com.blue.open.Code.Entity.Enums.MovimientoTipoEnum;
import com.blue.open.Code.Entity.Enums.ReservaAmenityCobroTipoEnum;
import com.blue.open.Code.Entity.Enums.ReservaEstadoEnum;
import com.blue.open.Code.Entity.Enums.SlotEstadoEnum;
import com.blue.open.Code.Repository.MovimientoRepository;
import com.blue.open.Code.Repository.ReservaRepository;
import com.blue.open.Code.Repository.SlotRepository;
import com.blue.open.Code.Service.Amenities.ReservaAmenityCobroSupport;
import com.blue.open.Code.Service.Amenities.ReservaAmenityMontoCalculator;
import com.blue.open.Core.Exceptions.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class MercadoPagoLiquidacionReservaAmenityStrategy implements MercadoPagoLiquidacionStrategy {

    private final MovimientoRepository movimientoRepository;
    private final ReservaRepository reservaRepository;
    private final SlotRepository slotRepository;

    @Override
    public MercadoPagoCobroTipoEnum getTipo() {
        return MercadoPagoCobroTipoEnum.RESERVA_AMENITY;
    }

    @Override
    public void liquidar(MercadoPagoCobroIntent intent) {
        MercadoPagoCobro mercadoPagoCobro = intent.getMercadoPagoCobro();
        Reserva reserva = mercadoPagoCobro.getReserva();
        if (reserva == null) {
            throw new CustomException(HttpStatus.CONFLICT, "Cobro sin reserva vinculada",
                    "MercadoPagoLiquidacionReservaAmenityStrategy | intentUuid: " + intent.getUuid());
        }

        ReservaAmenityCobroTipoEnum tipoCobro = mercadoPagoCobro.getReservaCobroTipo();
        if (tipoCobro == null) {
            throw new CustomException(HttpStatus.CONFLICT, "Cobro sin tipo de reserva amenity",
                    "MercadoPagoLiquidacionReservaAmenityStrategy | intentUuid: " + intent.getUuid());
        }

        Reserva reservaActualizada = reservaRepository.findByUuidAndCompanyIdWithDetails(
                        reserva.getUuid(), intent.getCompany().getId())
                .orElseThrow(() -> new CustomException(HttpStatus.CONFLICT, "Reserva no encontrada",
                        "MercadoPagoLiquidacionReservaAmenityStrategy | reservaUuid: " + reserva.getUuid()));

        String concepto = mercadoPagoCobro.getTitulo() != null && !mercadoPagoCobro.getTitulo().isBlank()
                ? mercadoPagoCobro.getTitulo()
                : ReservaAmenityCobroSupport.concepto(reservaActualizada, tipoCobro);

        if (tipoCobro == ReservaAmenityCobroTipoEnum.SENA) {
            liquidarSena(intent, mercadoPagoCobro, reservaActualizada, concepto);
            return;
        }

        if (tipoCobro == ReservaAmenityCobroTipoEnum.TOTAL) {
            liquidarTotal(intent, mercadoPagoCobro, reservaActualizada, concepto);
            return;
        }

        liquidarSaldo(intent, mercadoPagoCobro, reservaActualizada, concepto);
    }

    private void liquidarSena(
            MercadoPagoCobroIntent intent,
            MercadoPagoCobro mercadoPagoCobro,
            Reserva reserva,
            String concepto) {

        if (reserva.getEstado() != ReservaEstadoEnum.PENDIENTE_SENA) {
            if (reserva.getEstado() == ReservaEstadoEnum.CONFIRMADA && reserva.getSenaPagadaAt() != null) {
                return;
            }
            throw new CustomException(HttpStatus.CONFLICT, "La reserva no está pendiente de seña",
                    "MercadoPagoLiquidacionReservaAmenityStrategy | estado: " + reserva.getEstado());
        }

        guardarMovimiento(intent, mercadoPagoCobro, reserva, concepto);

        reserva.setSenaPagadaAt(new Timestamp(System.currentTimeMillis()));
        reserva.setEstado(ReservaEstadoEnum.CONFIRMADA);
        reserva.setSaldoPendiente(
                ReservaAmenityMontoCalculator.saldoTrasSena(reserva.getMontoTotal(), reserva.getMontoSena()));
        reservaRepository.save(reserva);

        Slot slot = reserva.getSlot();
        if (slot != null) {
            slot.setEstado(SlotEstadoEnum.CONFIRMADO);
            slotRepository.save(slot);
        }
    }

    private void liquidarTotal(
            MercadoPagoCobroIntent intent,
            MercadoPagoCobro mercadoPagoCobro,
            Reserva reserva,
            String concepto) {

        if (reserva.getEstado() != ReservaEstadoEnum.PENDIENTE_SENA) {
            if (reserva.getEstado() == ReservaEstadoEnum.CONFIRMADA
                    && reserva.getSenaPagadaAt() != null
                    && reserva.getSaldoPendiente().compareTo(BigDecimal.ZERO) <= 0) {
                return;
            }
            throw new CustomException(HttpStatus.CONFLICT, "La reserva no está pendiente de seña",
                    "MercadoPagoLiquidacionReservaAmenityStrategy | estado: " + reserva.getEstado());
        }

        guardarMovimiento(intent, mercadoPagoCobro, reserva, concepto);

        reserva.setSenaPagadaAt(new Timestamp(System.currentTimeMillis()));
        reserva.setEstado(ReservaEstadoEnum.CONFIRMADA);
        reserva.setSaldoPendiente(BigDecimal.ZERO);
        reservaRepository.save(reserva);

        Slot slot = reserva.getSlot();
        if (slot != null) {
            slot.setEstado(SlotEstadoEnum.CONFIRMADO);
            slotRepository.save(slot);
        }
    }

    private void liquidarSaldo(
            MercadoPagoCobroIntent intent,
            MercadoPagoCobro mercadoPagoCobro,
            Reserva reserva,
            String concepto) {

        if (reserva.getEstado() != ReservaEstadoEnum.CONFIRMADA) {
            throw new CustomException(HttpStatus.CONFLICT, "Solo se puede liquidar saldo de reservas confirmadas",
                    "MercadoPagoLiquidacionReservaAmenityStrategy | estado: " + reserva.getEstado());
        }

        if (reserva.getSaldoPendiente().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        guardarMovimiento(intent, mercadoPagoCobro, reserva, concepto);
        reserva.setSaldoPendiente(BigDecimal.ZERO);
        reservaRepository.save(reserva);
    }

    private void guardarMovimiento(
            MercadoPagoCobroIntent intent,
            MercadoPagoCobro mercadoPagoCobro,
            Reserva reserva,
            String concepto) {

        Movimiento movimiento = new Movimiento();
        movimiento.setCompany(intent.getCompany());
        movimiento.setTipo(MovimientoTipoEnum.INGRESO);
        movimiento.setEstado(MovimientoEstadoEnum.ACTIVO);
        movimiento.setMonto(mercadoPagoCobro.getMontoTotal());
        movimiento.setFecha(LocalDate.now());
        movimiento.setMedioPago(MedioPagoEnum.MERCADO_PAGO);
        movimiento.setReserva(reserva);
        movimiento.setPersona(reserva.getMember());
        movimiento.setTitular(reserva.getMember());
        movimiento.setConcepto(concepto);
        movimiento.setRegistradoPor(mercadoPagoCobro.getRegistradoPor());
        movimiento.setObservaciones("Mercado Pago | paymentId: " + intent.getPaymentId());

        movimientoRepository.save(movimiento);
    }
}
