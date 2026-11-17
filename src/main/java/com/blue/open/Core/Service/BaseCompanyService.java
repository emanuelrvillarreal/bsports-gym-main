package com.blue.open.Core.Service;

import com.blue.open.Code.Entity.DTO.CompanyContextDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Mapper.CompanyMapper;
import com.blue.open.Core.Entity.DTO.BaseCompanyDTO;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Repository.BaseCompanyRepository;
import com.blue.open.Core.Service.Auth.DatabaseInitializerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Transactional
public abstract class BaseCompanyService<DTO extends BaseCompanyDTO> extends AbstractService<Company, DTO, DTO> {

    @Autowired
    protected BaseCompanyRepository companyRepository;

    @Autowired
    private CompanyMapper companyMapper;

    @Value("${core.multi-company.enabled:true}")
    private boolean multiTenantEnabled;

    @Override
    protected PaginateList hookPaginatePreQuery(PaginateList paginateList) {
        paginateList.init("companies");
        return paginateList;
    }

    @Transactional(readOnly = true)
    public CompanyContextDTO getContext() {
        CompanyContextDTO context = new CompanyContextDTO();
        context.setMultiTenantEnabled(multiTenantEnabled);
        context.setDefaultCompanyUuid(DatabaseInitializerService.DEFAULT_COMPANY_UUID);
        context.setCompanyHeaderRequired(multiTenantEnabled);

        String requestCompanyUuid = null;
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes();
        if (requestAttributes != null) {
            requestCompanyUuid = requestAttributes.getRequest().getHeader("X-Company-Uuid");
        }
        context.setRequestCompanyUuid(requestCompanyUuid);
        context.setResolvedCompanyUuid(authService.resolveCompanyUuid(requestCompanyUuid));

        User user = authService.getActualOrNull();
        if (user != null) {
            try {
                Company currentCompany = authService.getCurrentCompanyOrThrow();
                context.setCurrentCompany(companyMapper.modelToDto(currentCompany));
            } catch (CustomException ignored) {
                // Sin header en modo multi-empresa: se devuelve solo la configuración
            }
        }

        return context;
    }

}
