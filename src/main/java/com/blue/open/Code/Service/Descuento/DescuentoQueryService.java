package com.blue.open.Code.Service.Descuento;

import com.blue.open.Code.Entity.DTO.DescuentoDTO;
import com.blue.open.Code.Entity.DTO.DescuentoPaginateRequestDTO;
import com.blue.open.Code.Entity.Db.Descuento;
import com.blue.open.Code.Mapper.DescuentoMapper;
import com.blue.open.Code.Repository.DescuentoRepository;
import com.blue.open.Code.Repository.DescuentoSpecifications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DescuentoQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    @Autowired
    private DescuentoRepository descuentoRepository;

    @Autowired
    private DescuentoMapper descuentoMapper;

    @Transactional(readOnly = true)
    public Page<DescuentoDTO> paginar(DescuentoPaginateRequestDTO request, Long companyId) {
        DescuentoPaginateRequestDTO req = request != null ? request : new DescuentoPaginateRequestDTO();
        int page = Math.max(req.getPage(), 1);
        int size = req.getSize() <= 0 ? 10 : Math.min(req.getSize(), MAX_PAGE_SIZE);

        Specification<Descuento> spec = Specification.where(DescuentoSpecifications.forCompany(companyId))
                .and(DescuentoSpecifications.withPaginateRequest(req));

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(
                Sort.Order.asc("titulo"),
                Sort.Order.asc("id")));

        return descuentoRepository.findAll(spec, pageable).map(descuentoMapper::toDto);
    }
}
