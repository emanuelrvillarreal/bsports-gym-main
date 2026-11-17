package com.blue.open.Code.Service.Tesoreria;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.MovimientoAnularCommand;
import com.blue.open.Code.Entity.DTO.MovimientoDetalleDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Cuota;
import com.blue.open.Code.Entity.Db.Movimiento;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.MovimientoEstadoEnum;
import com.blue.open.Code.Mapper.MovimientoMapper;
import com.blue.open.Code.Repository.CuotaRepository;
import com.blue.open.Code.Repository.MovimientoRepository;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Service
public class MovimientoAnulacionService {

    @Autowired
    private AuthService authService;

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Autowired
    private CuotaRepository cuotaRepository;

    @Autowired
    private MovimientoMapper movimientoMapper;

    @Autowired
    private MemberEstadoRecalculoService memberEstadoRecalculoService;

    @Transactional
    public MovimientoDetalleDTO anular(String movimientoUuid, MovimientoAnularCommand command) {
        Company company = authService.getCurrentCompanyOrThrow();
        User anuladoPor = authService.valid(List.of(PermissionEnum.TREASURY_VOID));
        Long companyId = company.getId();

        Movimiento movimiento = movimientoRepository.findByUuidAndCompanyIdWithDetails(movimientoUuid, companyId);
        if (movimiento == null) {
            throw new CustomException(HttpStatus.NOT_FOUND, "Movimiento no encontrado",
                    "MovimientoAnulacionService | movimientoUuid: " + movimientoUuid);
        }

        if (movimiento.getEstado() == MovimientoEstadoEnum.ANULADO) {
            throw new CustomException(HttpStatus.CONFLICT, "El movimiento ya está anulado",
                    "MovimientoAnulacionService | movimientoUuid: " + movimientoUuid);
        }

        movimiento.setEstado(MovimientoEstadoEnum.ANULADO);
        movimiento.setAnuladaPor(anuladoPor);
        movimiento.setAnuladaMotivo(command.getMotivo().trim());
        movimiento.setAnuladaAt(new Timestamp(System.currentTimeMillis()));

        Long titularIdAfectado = revertirCuotaSiAplica(movimiento);
        movimientoRepository.save(movimiento);

        if (titularIdAfectado != null) {
            memberEstadoRecalculoService.recalcularGruposPorTitularIds(companyId, Set.of(titularIdAfectado));
        }

        return movimientoMapper.toDetalle(movimiento);
    }

    private Long revertirCuotaSiAplica(Movimiento movimiento) {
        Cuota cuota = movimiento.getCuota();
        if (cuota == null) {
            return null;
        }
        cuota.revertirPago(LocalDate.now());
        cuotaRepository.save(cuota);
        return cuota.getTitular().getId();
    }
}
