package com.blue.open.Code.Service.Tesoreria;

import com.blue.open.Code.Entity.DTO.MovimientoDetalleDTO;
import com.blue.open.Code.Entity.DTO.MovimientoPaginateRequestDTO;
import com.blue.open.Code.Entity.DTO.MovimientoRowDTO;
import com.blue.open.Code.Entity.Db.Movimiento;
import com.blue.open.Code.Entity.Enums.MovimientoEstadoEnum;
import com.blue.open.Code.Mapper.MovimientoMapper;
import com.blue.open.Code.Repository.MovimientoRepository;
import com.blue.open.Code.Repository.MovimientoSpecifications;
import com.blue.open.Core.Exceptions.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MovimientoQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Autowired
    private MovimientoMapper movimientoMapper;

    @Transactional(readOnly = true)
    public Page<MovimientoRowDTO> paginar(MovimientoPaginateRequestDTO request, Long companyId) {
        MovimientoPaginateRequestDTO req = request != null ? request : new MovimientoPaginateRequestDTO();
        int page = Math.max(req.getPage(), 1);
        int size = req.getSize() <= 0 ? 20 : Math.min(req.getSize(), MAX_PAGE_SIZE);

        List<MovimientoEstadoEnum> estados = req.getEstados();
        if (estados == null || estados.isEmpty()) {
            estados = List.of(MovimientoEstadoEnum.ACTIVO);
        }

        MovimientoPaginateRequestDTO effective = new MovimientoPaginateRequestDTO();
        effective.setTipo(req.getTipo());
        effective.setEstados(estados);
        effective.setFechaDesde(req.getFechaDesde());
        effective.setFechaHasta(req.getFechaHasta());

        Specification<Movimiento> spec = Specification.where(MovimientoSpecifications.forCompany(companyId))
                .and(MovimientoSpecifications.withPaginateRequest(effective))
                .and(MovimientoSpecifications.fetchListDetails());

        Pageable pageable = PageRequest.of(page - 1, size,
                Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("id")));

        return movimientoRepository.findAll(spec, pageable).map(movimientoMapper::toRow);
    }

    @Transactional(readOnly = true)
    public MovimientoDetalleDTO obtenerPorUuid(String movimientoUuid, Long companyId) {
        Movimiento movimiento = movimientoRepository.findByUuidAndCompanyIdWithDetails(movimientoUuid, companyId);
        if (movimiento == null) {
            throw new CustomException(HttpStatus.NOT_FOUND, "Movimiento no encontrado",
                    "MovimientoQueryService | movimientoUuid: " + movimientoUuid);
        }
        return movimientoMapper.toDetalle(movimiento);
    }
}
