package com.blue.open.Code.Service.Amenities;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.Db.Amenity;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Reserva;
import com.blue.open.Code.Entity.Db.Slot;
import com.blue.open.Code.Entity.Enums.ReservaEstadoEnum;
import com.blue.open.Code.Entity.Enums.SlotEstadoEnum;
import com.blue.open.Code.Repository.AmenityRepository;
import com.blue.open.Code.Repository.ReservaRepository;
import com.blue.open.Code.Repository.SlotRepository;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AmenityBajaService {

    private static final int STATUS_DELETED = -1;

    @Autowired
    private AuthService authService;

    @Autowired
    private AmenityRepository amenityRepository;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Transactional
    public void desactivar(String uuid) {
        authService.valid(List.of(PermissionEnum.AMENITY_DELETE));
        Company company = authService.getCurrentCompanyOrThrow();

        Amenity amenity = amenityRepository.findByUuidAndCompanyIdAndStatusActive(uuid, company.getId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Amenity no encontrado",
                        "AmenityBajaService | uuid: " + uuid));

        if (amenity.getStatus() != null && amenity.getStatus() == STATUS_DELETED) {
            return;
        }

        List<SlotEstadoEnum> estadosFuturos = List.of(
                SlotEstadoEnum.PENDIENTE_DE_PAGO,
                SlotEstadoEnum.CONFIRMADO);

        List<Slot> slotsFuturos = slotRepository.findFuturosByAmenityId(
                amenity.getId(), LocalDateTime.now(), estadosFuturos);

        if (!slotsFuturos.isEmpty()) {
            for (Slot slot : slotsFuturos) {
                slot.setEstado(SlotEstadoEnum.CANCELADO);
            }
            slotRepository.saveAll(slotsFuturos);

            List<Long> slotIds = slotsFuturos.stream().map(Slot::getId).toList();
            List<Reserva> reservas = reservaRepository.findByCompanyIdAndSlotIdIn(company.getId(), slotIds);
            if (!reservas.isEmpty()) {
                for (Reserva reserva : reservas) {
                    reserva.setEstado(ReservaEstadoEnum.CANCELADA);
                    reserva.setSaldoPendiente(BigDecimal.ZERO);
                }
                reservaRepository.saveAll(reservas);
            }
        }

        amenity.setStatus(STATUS_DELETED);
        amenityRepository.save(amenity);
    }
}
