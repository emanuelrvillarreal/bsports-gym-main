package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Entity.DTO.ProfesorDTO;
import com.blue.open.Code.Entity.DTO.ProfesorPaginateRequestDTO;
import com.blue.open.Code.Entity.Db.Profesor;
import com.blue.open.Code.Mapper.ProfesorMapper;
import com.blue.open.Code.Repository.ProfesorRepository;
import com.blue.open.Code.Repository.ProfesorSpecifications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfesorQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    @Autowired
    private ProfesorRepository profesorRepository;

    @Autowired
    private ProfesorMapper profesorMapper;

    @Transactional(readOnly = true)
    public Page<ProfesorDTO> paginar(ProfesorPaginateRequestDTO request, Long companyId) {
        ProfesorPaginateRequestDTO req = request != null ? request : new ProfesorPaginateRequestDTO();
        int page = Math.max(req.getPage(), 1);
        int size = req.getSize() <= 0 ? 10 : Math.min(req.getSize(), MAX_PAGE_SIZE);

        Specification<Profesor> spec = Specification.where(ProfesorSpecifications.forCompany(companyId))
                .and(ProfesorSpecifications.withPaginateRequest(req))
                .and(ProfesorSpecifications.fetchUser());

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(
                Sort.Order.asc("user.apellido"),
                Sort.Order.asc("user.nombre"),
                Sort.Order.asc("id")));

        return profesorRepository.findAll(spec, pageable).map(profesorMapper::modelToDto);
    }
}
