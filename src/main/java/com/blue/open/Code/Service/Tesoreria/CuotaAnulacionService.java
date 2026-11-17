package com.blue.open.Code.Service.Tesoreria;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.ActividadInscripcionCommand;
import com.blue.open.Code.Entity.DTO.CuotaAnularCommand;
import com.blue.open.Code.Entity.DTO.CuotaDetalleDTO;
import com.blue.open.Code.Entity.DTO.MovimientoAnularCommand;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Cuota;
import com.blue.open.Code.Entity.Db.Movimiento;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.CuotaTipoEnum;
import com.blue.open.Code.Entity.Enums.MovimientoEstadoEnum;
import com.blue.open.Code.Mapper.CuotaMapper;
import com.blue.open.Code.Repository.CuotaRepository;
import com.blue.open.Code.Repository.MovimientoRepository;
import com.blue.open.Code.Service.Actividades.InscripcionActividadBajaService;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class CuotaAnulacionService {

    private static final int STATUS_ACTIVE = 0;

    @Autowired
    private AuthService authService;

    @Autowired
    private CuotaRepository cuotaRepository;

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Autowired
    private MovimientoAnulacionService movimientoAnulacionService;

    @Autowired
    private CuotaMapper cuotaMapper;

    @Autowired
    private MemberEstadoRecalculoService memberEstadoRecalculoService;

    @Autowired
    private InscripcionActividadBajaService inscripcionActividadBajaService;

    @Transactional
    public CuotaDetalleDTO anular(String cuotaUuid, CuotaAnularCommand command) {
        Company company = authService.getCurrentCompanyOrThrow();
        User anuladaPor = authService.valid(List.of(PermissionEnum.TREASURY_VOID));

        Cuota cuota = cuotaRepository.findByUuidAndCompanyIdWithDetails(cuotaUuid, company.getId());
        if (cuota == null) {
            throw new CustomException(HttpStatus.NOT_FOUND, "Cuota no encontrada",
                    "CuotaAnulacionService | cuotaUuid: " + cuotaUuid);
        }

        Optional<Movimiento> cobroActivo = movimientoRepository.findFirstByCuotaIdAndEstadoAndStatus(
                cuota.getId(), MovimientoEstadoEnum.ACTIVO, STATUS_ACTIVE);
        if (cobroActivo.isPresent()) {
            anularCobro(cobroActivo.get(), command);
        }

        cuota.anular(anuladaPor, command.getMotivo());
        cuotaRepository.save(cuota);

        if (command.isDarDeBajaActividad()) {
            darDeBajaDeLaActividad(cuota, command.getMotivo());
        }

        memberEstadoRecalculoService.recalcularGruposPorTitularIds(
                company.getId(), Set.of(cuota.getTitular().getId()));

        return cuotaMapper.toDetalle(cuota, LocalDate.now());
    }

    /** En la misma transacción: si la baja falla (por ejemplo, ya no estaba inscripto), tampoco se anula la cuota. */
    private void darDeBajaDeLaActividad(Cuota cuota, String motivo) {
        if (cuota.getTipo() != CuotaTipoEnum.ACTIVIDAD || cuota.getActividad() == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Solo las cuotas de actividad permiten dar de baja la inscripción",
                    "CuotaAnulacionService | cuotaId: " + cuota.getId() + " tipo: " + cuota.getTipo());
        }
        authService.valid(List.of(PermissionEnum.ACTIVITY_UNENROLL));
        ActividadInscripcionCommand baja = new ActividadInscripcionCommand();
        baja.setMemberUuid(cuota.getPersona().getUuid());
        String motivoBaja = "Cuota anulada: " + motivo;
        baja.setMotivoBaja(motivoBaja.length() > 255 ? motivoBaja.substring(0, 255) : motivoBaja);
        inscripcionActividadBajaService.darBaja(cuota.getActividad().getUuid(), baja);
    }

    private void anularCobro(Movimiento cobro, CuotaAnularCommand command) {
        MovimientoAnularCommand movimientoCommand = new MovimientoAnularCommand();
        movimientoCommand.setMotivo(command.getMotivo());
        movimientoAnulacionService.anular(cobro.getUuid(), movimientoCommand);
    }
}
