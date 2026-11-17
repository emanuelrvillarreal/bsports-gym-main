package com.blue.open.Code.Mapper;

import com.blue.open.Code.Entity.DTO.PagoDetalleDTO;
import com.blue.open.Code.Entity.DTO.PagoItemDTO;
import com.blue.open.Code.Entity.Db.MercadoPagoCobro;
import com.blue.open.Code.Entity.Db.MercadoPagoCobroIntent;
import com.blue.open.Code.Entity.Db.MercadoPagoCobroItem;
import org.springframework.stereotype.Component;

@Component
public class PagoMapper {

    public PagoDetalleDTO toDetalleDto(MercadoPagoCobro cobro) {
        PagoDetalleDTO dto = new PagoDetalleDTO();
        dto.setUuid(cobro.getUuid());
        dto.setTitulo(cobro.getTitulo());
        dto.setDetalle(cobro.getDetalle());
        dto.setMontoTotal(cobro.getMontoTotal());
        dto.setTipo(cobro.getTipo());
        mapIntent(dto, cobro.getMercadoPagoCobroIntent());

        for (MercadoPagoCobroItem item : cobro.getItems()) {
            dto.getItems().add(toItemDto(item));
        }
        return dto;
    }

    public PagoItemDTO toItemDto(MercadoPagoCobroItem item) {
        PagoItemDTO dto = new PagoItemDTO();
        dto.setUuid(item.getUuid());
        dto.setTitulo(item.getTitulo());
        dto.setDetalle(item.getDetalle());
        dto.setMonto(item.getMonto());
        if (item.getCuota() != null) {
            dto.setCuotaUuid(item.getCuota().getUuid());
        }
        return dto;
    }

    private void mapIntent(PagoDetalleDTO dto, MercadoPagoCobroIntent intent) {
        if (intent == null) {
            return;
        }
        dto.setExternalReference(intent.getExternalReference());
        dto.setEstado(intent.getEstado());
        dto.setPaymentId(intent.getPaymentId());
    }
}
