package com.blue.open.Code.Service.Amenities;

import com.blue.open.Code.Entity.DTO.ReservaAmenityDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Reserva;
import com.blue.open.Code.Entity.Db.Slot;
import com.blue.open.Code.Entity.Enums.ReservaEstadoEnum;
import com.blue.open.Code.Entity.Enums.SlotEstadoEnum;
import com.blue.open.Code.Mapper.AmenityMapper;
import com.blue.open.Code.Repository.ReservaRepository;
import com.blue.open.Code.Repository.SlotRepository;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Service
@RequiredArgsConstructor
public class ReservaAmenityConfirmacionService {

    private final AuthService authService;
    private final ReservaRepository reservaRepository;
    private final SlotRepository slotRepository;
    private final AmenityMapper amenityMapper;

    @Transactional
    public ReservaAmenityDTO confirmar(String reservaUuid) {
        Company company = authService.getCurrentCompanyOrThrow();

        Reserva reserva = reservaRepository.findByUuidAndCompanyIdWithDetails(reservaUuid, company.getId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Reserva no encontrada",
                        "ReservaAmenityConfirmacionService | uuid: " + reservaUuid));

        if (reserva.getMontoTotal().compareTo(BigDecimal.ZERO) > 0) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "La reserva tiene tarifa; use cobro-sena",
                    "ReservaAmenityConfirmacionService | montoTotal: " + reserva.getMontoTotal());
        }

        if (reserva.getEstado() != ReservaEstadoEnum.PENDIENTE_SENA) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "La reserva no está pendiente de confirmación",
                    "ReservaAmenityConfirmacionService | estado: " + reserva.getEstado());
        }

        if (reserva.getSenaPagadaAt() != null) {
            throw new CustomException(HttpStatus.CONFLICT, "La reserva ya fue confirmada",
                    "ReservaAmenityConfirmacionService | reservaUuid: " + reservaUuid);
        }

        reserva.setSenaPagadaAt(new Timestamp(System.currentTimeMillis()));
        reserva.setEstado(ReservaEstadoEnum.CONFIRMADA);
        reservaRepository.save(reserva);

        Slot slot = reserva.getSlot();
        slot.setEstado(SlotEstadoEnum.CONFIRMADO);
        slotRepository.save(slot);

        return amenityMapper.reservaToDto(reserva);
    }
}
