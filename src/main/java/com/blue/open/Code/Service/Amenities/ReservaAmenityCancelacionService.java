package com.blue.open.Code.Service.Amenities;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.ReservaAmenityDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Reserva;
import com.blue.open.Code.Entity.Db.Slot;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.ReservaEstadoEnum;
import com.blue.open.Code.Entity.Enums.SlotEstadoEnum;
import com.blue.open.Code.Mapper.AmenityMapper;
import com.blue.open.Code.Repository.ReservaRepository;
import com.blue.open.Code.Repository.SlotRepository;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservaAmenityCancelacionService {

    @Autowired
    private AuthService authService;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private AmenityMapper amenityMapper;

    @Transactional
    public ReservaAmenityDTO cancelar(String reservaUuid) {
        User canceladoPor = authService.valid(List.of(PermissionEnum.AMENITY_RESERVATION_CANCEL));
        Company company = authService.getCurrentCompanyOrThrow();

        Reserva reserva = reservaRepository.findByUuidAndCompanyIdWithDetails(reservaUuid, company.getId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Reserva no encontrada",
                        "ReservaAmenityCancelacionService | uuid: " + reservaUuid));

        if (reserva.getEstado() == ReservaEstadoEnum.CANCELADA) {
            throw new CustomException(HttpStatus.CONFLICT, "La reserva ya está cancelada",
                    "ReservaAmenityCancelacionService | uuid: " + reservaUuid);
        }

        if (reserva.getEstado() == ReservaEstadoEnum.VENCIDA) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "No se puede cancelar una reserva vencida",
                    "ReservaAmenityCancelacionService | uuid: " + reservaUuid);
        }

        if (reserva.getEstado() != ReservaEstadoEnum.PENDIENTE_SENA
                && reserva.getEstado() != ReservaEstadoEnum.CONFIRMADA) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Estado de reserva no cancelable",
                    "ReservaAmenityCancelacionService | estado: " + reserva.getEstado());
        }

        Slot slot = reserva.getSlot();
        if (slot.getFin() == null || !slot.getFin().isAfter(LocalDateTime.now())) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "No se puede cancelar una reserva con horario de uso ya finalizado",
                    "ReservaAmenityCancelacionService | slotFin: " + slot.getFin());
        }

        boolean slotYaCancelado = slot.getEstado() == SlotEstadoEnum.CANCELADO;
        if (!slotYaCancelado) {
            slot.setEstado(SlotEstadoEnum.CANCELADO);
            slotRepository.save(slot);
        }

        reserva.setEstado(ReservaEstadoEnum.CANCELADA);
        reserva.setSaldoPendiente(BigDecimal.ZERO);
        reserva.setCanceladaPor(canceladoPor);
        reserva.setCanceladaAt(new Timestamp(System.currentTimeMillis()));
        reservaRepository.save(reserva);

        return amenityMapper.reservaToDto(reserva);
    }
}
