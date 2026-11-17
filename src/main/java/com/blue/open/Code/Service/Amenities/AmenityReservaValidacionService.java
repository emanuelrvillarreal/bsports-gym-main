package com.blue.open.Code.Service.Amenities;

import com.blue.open.Code.Entity.Db.Amenity;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Enums.ReservaEstadoEnum;
import com.blue.open.Code.Entity.Enums.SlotEstadoEnum;
import com.blue.open.Code.Repository.ReservaRepository;
import com.blue.open.Code.Repository.SlotRepository;
import com.blue.open.Code.Service.MemberAuthService;
import com.blue.open.Code.Util.DiasSemanaUtil;
import com.blue.open.Core.Exceptions.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class AmenityReservaValidacionService {

    private static final int MAX_RESERVAS_SEMANA = 2;

    @Autowired
    private MemberAuthService memberAuthService;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private SlotRepository slotRepository;

    public Member validarTitular(String memberUuid, Long companyId) {
        Member member = memberAuthService.resolveMemberByUuid(memberUuid, companyId);
        if (!member.isTitular()) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "El identificador no corresponde a un socio titular",
                    "AmenityReservaValidacionService | uuid: " + memberUuid);
        }
        return member;
    }

    public void validarTopeSemanal(Long companyId, Long memberId, LocalDateTime inicio) {
        LocalDate slotDate = inicio.toLocalDate();
        LocalDate semanaInicioDate = slotDate.with(DayOfWeek.MONDAY);
        LocalDate semanaFinDate = semanaInicioDate.plusDays(7);

        LocalDateTime semanaInicio = semanaInicioDate.atStartOfDay();
        LocalDateTime semanaFin = semanaFinDate.atStartOfDay();

        List<ReservaEstadoEnum> estadosActivos = List.of(
                ReservaEstadoEnum.PENDIENTE_SENA,
                ReservaEstadoEnum.CONFIRMADA);

        long count = reservaRepository.countByMemberAndSemanaCalendario(
                companyId, memberId, estadosActivos, semanaInicio, semanaFin);

        if (count >= MAX_RESERVAS_SEMANA) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "El titular ya alcanzó el máximo de 2 reservas de amenities por semana",
                    "AmenityReservaValidacionService | memberId: " + memberId);
        }
    }

    public void validarHorarioAmenity(Amenity amenity, LocalDateTime inicio, LocalDateTime fin) {
        if (!fin.isAfter(inicio)) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "El fin del slot debe ser posterior al inicio",
                    "AmenityReservaValidacionService | inicio/fin inválidos");
        }

        if (!DiasSemanaUtil.matchesDay(amenity.getDiasSemana(), inicio.getDayOfWeek())) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "El día del slot no está disponible para este amenity",
                    "AmenityReservaValidacionService | dia: " + inicio.getDayOfWeek());
        }

        LocalTime horaInicio = inicio.toLocalTime();
        LocalTime horaFin = fin.toLocalTime();

        if (horaInicio.isBefore(amenity.getHoraInicio()) || horaFin.isAfter(amenity.getHoraFin())) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "El horario del slot debe estar dentro del rango disponible del amenity",
                    "AmenityReservaValidacionService | horario fuera de rango");
        }
    }

    public void validarSolapamiento(Long amenityId, LocalDateTime inicio, LocalDateTime fin) {
        List<SlotEstadoEnum> estadosBloqueantes = List.of(
                SlotEstadoEnum.PENDIENTE_DE_PAGO,
                SlotEstadoEnum.CONFIRMADO);

        if (slotRepository.existsSolapamientoAmenity(amenityId, estadosBloqueantes, inicio, fin)) {
            throw new CustomException(HttpStatus.CONFLICT,
                    "Ya existe un slot solapado para este amenity",
                    "AmenityReservaValidacionService | amenityId: " + amenityId);
        }
    }
}
