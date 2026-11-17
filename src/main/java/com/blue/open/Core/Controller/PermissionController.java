package com.blue.open.Core.Controller;

import com.blue.open.Core.Auth.BasePermissionEnum;
import com.blue.open.Core.Entity.Db.Permission;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Entity.DTO.PermissionDTO;
import com.blue.open.Core.Service.PermissionService;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("permission")
public class PermissionController
        extends AbstractController<Permission, PermissionDTO, PermissionDTO, PermissionService> {

    @Autowired
    protected AuthService authService;

    @Override
    public void hookSaveAuth(PermissionDTO request) {
        if (request.getUuid() == null) {
            authService.valid(List.of(BasePermissionEnum.PERMISSION_CREATE));
        } else {
            authService.valid(List.of(BasePermissionEnum.PERMISSION_EDIT));
        }
    }

    @Override
    public void hookDeleteAuth(String uuid) {
        authService.valid(List.of(BasePermissionEnum.PERMISSION_DELETE));
    }

    @Override
    public void hookPaginateAuth(PaginateList paginateList) {
        authService.valid(List.of(BasePermissionEnum.PERMISSION_VIEW));
    }

    @Override
    public PaginateList hookPaginate(PaginateList paginateList) {
        return paginateList;
    }
}
