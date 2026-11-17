package com.blue.open.Core.Controller;

import com.blue.open.Core.Entity.Db.Role;
import com.blue.open.Code.Service.RoleService;
import com.blue.open.Core.Entity.DTO.RoleDTO;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("role")
public class RoleController extends AbstractController<Role, RoleDTO, RoleDTO, RoleService> {
}
