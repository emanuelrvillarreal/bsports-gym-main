package com.blue.open.Code.Service.Amenities;

import com.blue.open.Code.Entity.DTO.ReservaVencimientoResultDTO;
import com.blue.open.Code.Entity.Db.Reserva;
import com.blue.open.Code.Entity.Db.Slot;
import com.blue.open.Code.Entity.Enums.ReservaEstadoEnum;
import com.blue.open.Code.Repository.ReservaRepository;
import com.blue.open.Code.Repository.SlotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReservaAmenityVencimientoService {

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private SlotRepository slotRepository;

    @Transactional
    public ReservaVencimientoResultDTO procesarVencidas() {
        LocalDateTime ahora = LocalDateTime.now();
        List<Reserva> vencidas = reservaRepository.findPendientesSenaVencidas(
                ReservaEstadoEnum.PENDIENTE_SENA, ahora);

        int slotsEliminados = 0;
        for (Reserva reserva : vencidas) {
            reserva.setEstado(ReservaEstadoEnum.VENCIDA);
            reservaRepository.save(reserva);

            Slot slot = reserva.getSlot();
            if (slot != null) {
                slotRepository.delete(slot);
                slotsEliminados++;
            }
        }

        ReservaVencimientoResultDTO result = new ReservaVencimientoResultDTO();
        result.setReservasVencidas(vencidas.size());
        result.setSlotsEliminados(slotsEliminados);
        result.setProcesadoAt(ahora);
        return result;
    }
}
