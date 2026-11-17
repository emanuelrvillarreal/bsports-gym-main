package com.blue.open.Code.Service.Descuento;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Descuento;
import com.blue.open.Code.Repository.DescuentoRepository;
import com.blue.open.Code.Repository.InscripcionActividadDescuentoRepository;
import com.blue.open.Code.Repository.MemberDescuentoRepository;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DescuentoBajaService {

    @Autowired
    private AuthService authService;

    @Autowired
    private DescuentoRepository descuentoRepository;

    @Autowired
    private MemberDescuentoRepository memberDescuentoRepository;

    @Autowired
    private InscripcionActividadDescuentoRepository inscripcionActividadDescuentoRepository;

    /**
     * Baja lógica del catálogo. Antes de marcarlo, elimina físicamente todas las asignaciones
     * (MemberDescuento / InscripcionActividadDescuento) que apunten a este descuento — síncrono,
     * en la misma transacción, para que no quede una cuota "fantasma" apuntando a un descuento
     * ya dado de baja. Las cuotas ya generadas no se ven afectadas: guardan su propio snapshot
     * (CuotaDescuento), sin referencia viva a la asignación ni al descuento.
     */
    @Transactional
    public void bajaLogica(String descuentoUuid) {
        Company company = authService.getCurrentCompanyOrThrow();

        Descuento descuento = descuentoRepository.findByUuidAndCompanyIdAndStatusActive(descuentoUuid, company.getId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Descuento no encontrado",
                        "DescuentoBajaService | descuentoUuid: " + descuentoUuid));

        memberDescuentoRepository.deleteAllByDescuentoId(descuento.getId());
        inscripcionActividadDescuentoRepository.deleteAllByDescuentoId(descuento.getId());

        descuento.darDeBaja();
        descuentoRepository.save(descuento);
    }
}
