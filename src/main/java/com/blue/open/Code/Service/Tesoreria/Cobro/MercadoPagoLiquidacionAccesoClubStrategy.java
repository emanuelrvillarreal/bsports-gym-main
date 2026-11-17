package com.blue.open.Code.Service.Tesoreria.Cobro;

import com.blue.open.Code.Entity.Db.EventoAcceso;
import com.blue.open.Code.Entity.Db.EventoAccesoMercadoPagoCobroDatos;
import com.blue.open.Code.Entity.Db.MercadoPagoCobro;
import com.blue.open.Code.Entity.Db.MercadoPagoCobroIntent;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.Movimiento;
import com.blue.open.Code.Entity.Enums.EventoAccesoHistorialTipoEnum;
import com.blue.open.Code.Entity.Enums.EventoAccesoScanEstadoEnum;
import com.blue.open.Code.Entity.Enums.MedioPagoEnum;
import com.blue.open.Code.Entity.Enums.MercadoPagoCobroTipoEnum;
import com.blue.open.Code.Entity.Enums.MovimientoEstadoEnum;
import com.blue.open.Code.Entity.Enums.MovimientoTipoEnum;
import com.blue.open.Code.Repository.MovimientoRepository;
import com.blue.open.Code.Service.EventoAcceso.EventoAccesoCobroSupport;
import com.blue.open.Code.Service.EventoAcceso.EventoAccesoHistorialService;
import com.blue.open.Core.Exceptions.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class MercadoPagoLiquidacionAccesoClubStrategy implements MercadoPagoLiquidacionStrategy {

    private final MovimientoRepository movimientoRepository;
    private final EventoAccesoHistorialService eventoAccesoHistorialService;

    @Override
    public MercadoPagoCobroTipoEnum getTipo() {
        return MercadoPagoCobroTipoEnum.ACCESO_CLUB;
    }

    @Override
    public void liquidar(MercadoPagoCobroIntent intent) {
        MercadoPagoCobro mercadoPagoCobro = intent.getMercadoPagoCobro();
        EventoAcceso evento = mercadoPagoCobro.getEventoAcceso();
        if (evento == null) {
            throw new CustomException(HttpStatus.CONFLICT, "Cobro sin evento de acceso vinculado",
                    "MercadoPagoLiquidacionAccesoClubStrategy | intentUuid: " + intent.getUuid());
        }

        String concepto = mercadoPagoCobro.getTitulo() != null && !mercadoPagoCobro.getTitulo().isBlank()
                ? mercadoPagoCobro.getTitulo()
                : EventoAccesoCobroSupport.conceptoInvitadoFallback(evento);

        Movimiento movimiento = new Movimiento();
        movimiento.setCompany(intent.getCompany());
        movimiento.setTipo(MovimientoTipoEnum.INGRESO);
        movimiento.setEstado(MovimientoEstadoEnum.ACTIVO);
        movimiento.setMonto(mercadoPagoCobro.getMontoTotal());
        movimiento.setFecha(LocalDate.now());
        movimiento.setMedioPago(MedioPagoEnum.MERCADO_PAGO);
        movimiento.setEventoAcceso(evento);
        movimiento.setConcepto(concepto);
        movimiento.setRegistradoPor(mercadoPagoCobro.getRegistradoPor());
        movimiento.setObservaciones("Mercado Pago | paymentId: " + intent.getPaymentId());

        EventoAccesoMercadoPagoCobroDatos datos = mercadoPagoCobro.getEventoAccesoDatos();
        boolean esSocio = datos != null && datos.getTipo() == EventoAccesoHistorialTipoEnum.SOCIO
                && datos.getMember() != null;

        if (esSocio) {
            Member member = datos.getMember();
            Member titular = member.isTitular() ? member : member.getTitular();
            movimiento.setPersona(member);
            movimiento.setTitular(titular);
        }

        movimientoRepository.save(movimiento);

        if (datos != null) {
            if (esSocio) {
                eventoAccesoHistorialService.registrarSocio(intent.getCompany(), evento, datos.getMember(),
                        datos.getNombre(), EventoAccesoScanEstadoEnum.ACCESO_PERMITIDO, concepto,
                        mercadoPagoCobro.getMontoTotal());
            } else {
                eventoAccesoHistorialService.registrarInvitado(intent.getCompany(), evento, datos.getNombre(),
                        EventoAccesoScanEstadoEnum.ACCESO_PERMITIDO, concepto, mercadoPagoCobro.getMontoTotal());
            }
        }
    }
}
