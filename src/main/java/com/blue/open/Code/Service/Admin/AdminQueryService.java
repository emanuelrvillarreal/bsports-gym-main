package com.blue.open.Code.Service.Admin;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.AdminDTO;
import com.blue.open.Code.Entity.DTO.AdminPaginateRequestDTO;
import com.blue.open.Code.Mapper.AdminMapper;
import com.blue.open.Code.Repository.UserCompanyRoleSpecifications;
import com.blue.open.Core.Entity.Db.UserCompanyRole;
import com.blue.open.Core.Repository.UserCompanyRoleRepository;
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
public class AdminQueryService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String ROLE_NAME_ADMIN = "ADMIN";

    @Autowired
    private AuthService authService;

    @Autowired
    private UserCompanyRoleRepository userCompanyRoleRepository;

    @Autowired
    private AdminMapper adminMapper;

    @Transactional(readOnly = true)
    public Page<AdminDTO> paginar(AdminPaginateRequestDTO request, Long companyId) {
        authService.valid(List.of(PermissionEnum.CLUB_INTERNAL_USER_MANAGE));
        AdminPaginateRequestDTO req = request != null ? request : new AdminPaginateRequestDTO();
        int page = Math.max(req.getPage(), 1);
        int size = req.getSize() <= 0 ? 10 : Math.min(req.getSize(), MAX_PAGE_SIZE);

        Specification<UserCompanyRole> spec = Specification
                .where(UserCompanyRoleSpecifications.forCompanyAndRole(companyId, ROLE_NAME_ADMIN))
                .and(UserCompanyRoleSpecifications.withSearch(req.getSearch()))
                .and(UserCompanyRoleSpecifications.fetchUser());

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(
                Sort.Order.asc("user.apellido"),
                Sort.Order.asc("user.nombre"),
                Sort.Order.asc("id")));

        return userCompanyRoleRepository.findAll(spec, pageable).map(adminMapper::modelToDto);
    }
}
