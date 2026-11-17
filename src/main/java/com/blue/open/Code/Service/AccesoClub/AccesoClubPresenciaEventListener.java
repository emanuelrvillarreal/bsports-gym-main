package com.blue.open.Code.Service.AccesoClub;

import com.blue.open.Code.Entity.DTO.AccesoClubItemDTO;
import com.blue.open.Code.Entity.DTO.AccesoClubPresenciaUpdateDTO;
import com.blue.open.Code.Entity.DTO.AccesoClubRegistroDTO;
import com.blue.open.Code.Event.AccesoClubRegistroEvent;
import com.blue.open.Code.Repository.ClubPresenciaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccesoClubPresenciaEventListener {

    private final AccesoClubPresenciaLiveService accesoClubPresenciaLiveService;
    private final ClubPresenciaRepository clubPresenciaRepository;

    @Async("AccesoClubEventExecutor")
    @EventListener
    public void onAccesoClubRegistro(AccesoClubRegistroEvent event) {
        try {
            log.info("AccesoClubPresenciaEventListener.onAccesoClubRegistro fue llamado para company {}", event.getCompanyId());
            Long companyId = event.getCompanyId();
            Long presenteCount = clubPresenciaRepository.countByCompanyIdAndPresente(companyId, true);
            log.info("Presente count obtenido: {}", presenteCount);

            AccesoClubPresenciaUpdateDTO update = new AccesoClubPresenciaUpdateDTO(
                    convertToItemDto(event.getRegistro()),
                    presenteCount.intValue()
            );

            log.info("Notificando a {} suscriptores de company {}", 0, companyId);
            accesoClubPresenciaLiveService.notificar(companyId, update);
            log.info("Notificación completada exitosamente");
        } catch (Exception ex) {
            log.error("Error en AccesoClubPresenciaEventListener.onAccesoClubRegistro", ex);
        }
    }

    private AccesoClubItemDTO convertToItemDto(AccesoClubRegistroDTO registro) {
        AccesoClubItemDTO item = new AccesoClubItemDTO();
        item.setAccesoUuid(registro.getAccesoUuid());
        item.setUserUuid(registro.getUserUuid());
        item.setNombre(registro.getNombre());
        item.setApellido(registro.getApellido());
        item.setDni(registro.getDni());
        item.setTipo(registro.getTipo());
        item.setMemberEstado(registro.getMemberEstado());
        item.setFecha(registro.getFecha());
        return item;
    }
}
