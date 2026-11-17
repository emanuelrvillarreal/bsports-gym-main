package com.blue.open.Code.Service.Recepcionista;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.RecepcionistaDTO;
import com.blue.open.Code.Entity.DTO.RecepcionistaPaginateRequestDTO;
import com.blue.open.Code.Entity.Db.Recepcionista;
import com.blue.open.Code.Mapper.RecepcionistaMapper;
import com.blue.open.Code.Repository.RecepcionistaRepository;
import com.blue.open.Code.Repository.RecepcionistaSpecifications;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RecepcionistaQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    @Autowired
    private AuthService authService;

    @Autowired
    private RecepcionistaRepository recepcionistaRepository;

    @Autowired
    private RecepcionistaMapper recepcionistaMapper;

    @Transactional(readOnly = true)
    public Page<RecepcionistaDTO> paginar(RecepcionistaPaginateRequestDTO request, Long companyId) {
        authService.valid(List.of(PermissionEnum.CLUB_INTERNAL_USER_MANAGE));
        RecepcionistaPaginateRequestDTO req = request != null ? request : new RecepcionistaPaginateRequestDTO();
        int page = Math.max(req.getPage(), 1);
        int size = req.getSize() <= 0 ? 10 : Math.min(req.getSize(), MAX_PAGE_SIZE);

        Specification<Recepcionista> spec = Specification.where(RecepcionistaSpecifications.forCompany(companyId))
                .and(RecepcionistaSpecifications.withPaginateRequest(req))
                .and(RecepcionistaSpecifications.fetchUser());

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(
                Sort.Order.asc("user.apellido"),
                Sort.Order.asc("user.nombre"),
                Sort.Order.asc("id")));

        return recepcionistaRepository.findAll(spec, pageable).map(recepcionistaMapper::modelToDto);
    }
}
