package com.blue.open.Code.Service.CuotaSocialRango;

import com.blue.open.Code.Entity.DTO.CuotaSocialRangoDTO;
import com.blue.open.Code.Entity.DTO.CuotaSocialRangoPaginateRequestDTO;
import com.blue.open.Code.Entity.Db.CuotaSocialRango;
import com.blue.open.Code.Mapper.CuotaSocialRangoMapper;
import com.blue.open.Code.Repository.CuotaSocialRangoRepository;
import com.blue.open.Code.Repository.CuotaSocialRangoSpecifications;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CuotaSocialRangoQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    @Autowired
    private AuthService authService;

    @Autowired
    private CuotaSocialRangoRepository cuotaSocialRangoRepository;

    @Autowired
    private CuotaSocialRangoMapper cuotaSocialRangoMapper;

    @Transactional(readOnly = true)
    public Page<CuotaSocialRangoDTO> paginar(CuotaSocialRangoPaginateRequestDTO request, Long companyId) {
        CuotaSocialRangoPaginateRequestDTO req = request != null ? request : new CuotaSocialRangoPaginateRequestDTO();
        int page = Math.max(req.getPage(), 1);
        int size = req.getSize() <= 0 ? 10 : Math.min(req.getSize(), MAX_PAGE_SIZE);

        Specification<CuotaSocialRango> spec = Specification.where(CuotaSocialRangoSpecifications.forCompany(companyId))
                .and(CuotaSocialRangoSpecifications.withPaginateRequest(req));

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(
                Sort.Order.asc("edadHasta"),
                Sort.Order.asc("id")));

        return cuotaSocialRangoRepository.findAll(spec, pageable).map(cuotaSocialRangoMapper::modelToDto);
    }
}
