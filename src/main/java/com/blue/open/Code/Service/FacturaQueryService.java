package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.DTO.FacturaPaginateRequestDTO;
import com.blue.open.Code.Entity.DTO.FacturaPaginateRowDTO;
import com.blue.open.Code.Entity.Db.Factura;
import com.blue.open.Code.Entity.Enums.FacturaOrdenEnum;
import com.blue.open.Code.Repository.FacturaRepository;
import com.blue.open.Code.Repository.FacturaSpecifications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
public class FacturaQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    @Autowired
    private FacturaRepository facturaRepository;

    @Transactional(readOnly = true)
    public Page<FacturaPaginateRowDTO> paginar(FacturaPaginateRequestDTO request, Long companyId) {
        FacturaPaginateRequestDTO req = request != null ? request : new FacturaPaginateRequestDTO();
        int page = Math.max(req.getPage(), 1);
        int size = req.getSize() <= 0 ? 10 : Math.min(req.getSize(), MAX_PAGE_SIZE);

        Specification<Factura> spec = Specification.where(FacturaSpecifications.forCompany(companyId))
                .and(FacturaSpecifications.withEstados(req.getEstados()));

        Pageable pageable = PageRequest.of(page - 1, size, toSort(req.getOrden()));
        return facturaRepository.findAll(spec, pageable).map(this::toRow);
    }

    private Sort toSort(FacturaOrdenEnum orden) {
        if (orden == FacturaOrdenEnum.FECHA_VENCIMIENTO_ASC) {
            return Sort.by(Sort.Direction.ASC, "fechaVencimiento");
        }
        return Sort.by(Sort.Direction.DESC, "fechaVencimiento");
    }

    private FacturaPaginateRowDTO toRow(Factura factura) {
        FacturaPaginateRowDTO row = new FacturaPaginateRowDTO();
        row.setFacturaUuid(factura.getUuid());
        row.setUuidFacturaCrm(factura.getUuidFacturaCrm());
        row.setCodigoPlan(factura.getCodigoPlan());
        row.setCodigosProductos(factura.getCodigosProductos() == null
                ? List.of()
                : Arrays.asList(factura.getCodigosProductos().split(",")));
        row.setImporte(factura.getImporte());
        row.setFechaGeneracion(factura.getFechaGeneracion());
        row.setFechaVencimiento(factura.getFechaVencimiento());
        row.setEstado(factura.getEstado());
        return row;
    }
}
