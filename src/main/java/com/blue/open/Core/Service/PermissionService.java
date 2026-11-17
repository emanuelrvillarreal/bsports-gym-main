package com.blue.open.Core.Service;

import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Entity.DTO.PermissionDTO;
import com.blue.open.Core.Entity.Db.Permission;
import com.blue.open.Core.Repository.PermissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Transactional
@Service
public class PermissionService extends AbstractService<Permission, PermissionDTO, PermissionDTO> {

    @Autowired
    protected PermissionRepository repository;

    @Override
    protected PaginateList hookPaginatePreQuery(PaginateList paginateList) {
        paginateList.init("auth_permissions");
        return paginateList;
    }

    @Override
    protected List<PermissionDTO> hookPaginateFilterResults(List<PermissionDTO> dtoList) {
        User currentUser = authService.getActualOrThrow();
        if (authService.isSysAdmin(currentUser)) {
            return dtoList;
        }

        String companyUuid = authService.getCurrentCompanyOrThrow().getUuid();
        Set<String> requesterPerms = authService.getAggregatedPermissions(currentUser, companyUuid);

        return dtoList.stream()
                .filter(p -> requesterPerms.contains(p.getName()))
                .collect(Collectors.toList());
    }
}
