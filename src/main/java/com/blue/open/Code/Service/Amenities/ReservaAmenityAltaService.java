package com.blue.open.Code.Service.Amenities;

import com.blue.open.Code.Entity.DTO.ReservaAmenityAltaCommand;
import com.blue.open.Code.Entity.DTO.ReservaAmenityDTO;
import com.blue.open.Code.Entity.DTO.ReservaAmenityMontoResult;
import com.blue.open.Code.Entity.Db.Amenity;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.Reserva;
import com.blue.open.Code.Entity.Db.Slot;
import com.blue.open.Code.Entity.Enums.ReservaEstadoEnum;
import com.blue.open.Code.Entity.Enums.SlotEstadoEnum;
import com.blue.open.Code.Mapper.AmenityMapper;
import com.blue.open.Code.Repository.AmenityRepository;
import com.blue.open.Code.Repository.ReservaRepository;
import com.blue.open.Code.Repository.SlotRepository;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Service
public class ReservaAmenityAltaService {

    private static final int HORAS_VENCIMIENTO_SENA = 48;

    @Autowired
    private AuthService authService;

    @Autowired
    private AmenityRepository amenityRepository;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private AmenityReservaValidacionService validacionService;

    @Autowired
    private AmenityMapper amenityMapper;

    @Transactional
    public ReservaAmenityDTO crear(ReservaAmenityAltaCommand command) {
        Company company = authService.getCurrentCompanyOrThrow();

        Amenity amenity = amenityRepository.findByUuidAndCompanyIdAndStatusActive(
                        command.getAmenityUuid(), company.getId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Amenity no encontrado",
                        "ReservaAmenityAltaService | uuid: " + command.getAmenityUuid()));

        Member titular = validacionService.validarTitular(command.getMemberUuid(), company.getId());
        validacionService.validarTopeSemanal(company.getId(), titular.getId(), command.getInicio());
        validacionService.validarHorarioAmenity(amenity, command.getInicio(), command.getFin());
        validacionService.validarSolapamiento(amenity.getId(), command.getInicio(), command.getFin());

        ReservaAmenityMontoResult montos = ReservaAmenityMontoCalculator.calcular(
                amenity, command.getInicio(), command.getFin());

        Slot slot = new Slot();
        slot.setCompany(company);
        slot.setActividad(null);
        slot.setAmenity(amenity);
        slot.setInicio(command.getInicio());
        slot.setFin(command.getFin());
        slot.setEstado(SlotEstadoEnum.PENDIENTE_DE_PAGO);
        slot = slotRepository.save(slot);

        LocalDateTime venceSena = LocalDateTime.now().plusHours(HORAS_VENCIMIENTO_SENA);

        Reserva reserva = new Reserva();
        reserva.setCompany(company);
        reserva.setAmenity(amenity);
        reserva.setSlot(slot);
        reserva.setMember(titular);
        reserva.setMontoTotal(montos.getMontoTotal());
        reserva.setMontoSena(montos.getMontoSena());
        reserva.setSaldoPendiente(montos.getSaldoPendiente());
        reserva.setVenceSenaAt(Timestamp.valueOf(venceSena));
        reserva.setEstado(ReservaEstadoEnum.PENDIENTE_SENA);
        reserva = reservaRepository.save(reserva);

        return amenityMapper.reservaToDto(reserva);
    }
}
