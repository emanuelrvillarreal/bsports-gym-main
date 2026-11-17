package com.blue.open.Code.Service.Categorias;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.CategoriaDTO;
import com.blue.open.Code.Entity.DTO.CategoriaPaginateRequestDTO;
import com.blue.open.Code.Entity.Db.Categoria;
import com.blue.open.Code.Mapper.CategoriaMapper;
import com.blue.open.Code.Repository.CategoriaRepository;
import com.blue.open.Code.Repository.CategoriaSpecifications;
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
public class CategoriaQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    @Autowired
    private AuthService authService;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private CategoriaMapper categoriaMapper;

    @Transactional(readOnly = true)
    public Page<CategoriaDTO> paginar(CategoriaPaginateRequestDTO request, Long companyId) {
        CategoriaPaginateRequestDTO req = request != null ? request : new CategoriaPaginateRequestDTO();
        int page = Math.max(req.getPage(), 1);
        int size = req.getSize() <= 0 ? 10 : Math.min(req.getSize(), MAX_PAGE_SIZE);

        Specification<Categoria> spec = Specification.where(CategoriaSpecifications.forCompany(companyId))
                .and(CategoriaSpecifications.withPaginateRequest(req));

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(
                Sort.Order.asc("nombre"),
                Sort.Order.asc("id")));

        return categoriaRepository.findAll(spec, pageable).map(categoriaMapper::modelToDto);
    }

    @Transactional(readOnly = true)
    public List<CategoriaDTO> listarActivas(Long companyId) {
        authService.valid(List.of(PermissionEnum.CATEGORIA_VIEW));
        return categoriaMapper.modelsToDtos(categoriaRepository.findActivasByCompanyId(companyId));
    }
}
