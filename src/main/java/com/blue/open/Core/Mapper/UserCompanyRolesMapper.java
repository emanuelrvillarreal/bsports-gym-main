package com.blue.open.Core.Mapper;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Service.CompanyAccesoService;
import com.blue.open.Core.Entity.Db.Role;
import com.blue.open.Core.Entity.Db.UserCompanyRole;
import com.blue.open.Core.Entity.DTO.RoleDTO;
import com.blue.open.Core.Entity.DTO.UserCompanyRoleDTO;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Repository.BaseCompanyRepository;
import com.blue.open.Core.Repository.RoleRepository;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserCompanyRolesMapper
        extends AbstractMapperComplete<UserCompanyRole, UserCompanyRoleDTO, UserCompanyRoleDTO> {

    @Autowired
    protected RoleMapper roleMapper;

    @Autowired
    private BaseCompanyRepository companyRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private CompanyAccesoService companyAccesoService;

    @Override
    public UserCompanyRoleDTO modelToDto(UserCompanyRole model) {
        try {
            UserCompanyRoleDTO dto = new UserCompanyRoleDTO();
            dto.setAbstract(model);

            if (model.getCompany() != null) {
                dto.setCompanyUuid(model.getCompany().getUuid());
                dto.setCompanyName(model.getCompany().getName());
                dto.setCompanyCuit(model.getCompany().getCuit());
                dto.setLogoUrl(model.getCompany().getLogoUrl());
                dto.setCompanyBlocked(companyAccesoService.estaBloqueada(model.getCompany()));
            }

            if (model.getRoles() != null) {
                dto.setRoles(roleMapper.modelsToDtos(model.getRoles()));
            }

            return dto;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException("Error al convertir los datos",
                    "Error en el mapper modelToDto " + e.getMessage());
        }
    }

    @Override
    public UserCompanyRole dtoToModel(UserCompanyRoleDTO userCompanyRoleDTO, UserCompanyRole model) {
        if (model == null) {
            model = new UserCompanyRole();
        }
        model.setAbstract(userCompanyRoleDTO);

        Company company = null;
        if (userCompanyRoleDTO.getCompanyUuid() != null) {
            company = companyRepository.findFirstByUuidWithStatusIsActive(userCompanyRoleDTO.getCompanyUuid());
        } else {
            company = authService.getCurrentCompanyOrThrow();
        }

        if (company != null) {
            model.setCompany(company);

            if (userCompanyRoleDTO.getRoles() != null) {
                model.getRoles().clear();
                for (RoleDTO roleDto : userCompanyRoleDTO.getRoles()) {
                    Role role = null;
                    if (roleDto.getUuid() != null) {
                        role = roleRepository.findFirstByUuid(roleDto.getUuid());
                    } else if (roleDto.getName() != null) {
                        role = roleRepository.findFirstByNameAndCompany(roleDto.getName(), company);
                    }
                    if (role != null) {
                        model.getRoles().add(role);
                    }
                }
            }
        }

        return model;
    }

    @Override
    public UserCompanyRoleDTO modelToPaginateDto(UserCompanyRole model) {
        return modelToDto(model);
    }

}
