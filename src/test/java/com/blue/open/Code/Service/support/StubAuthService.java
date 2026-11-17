package com.blue.open.Code.Service.support;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.http.HttpStatus;

import java.util.List;

public class StubAuthService extends AuthService {

    private boolean allowAccess = true;
    private Company company = new Company();
    private User user = new User();

    public void setAllowAccess(boolean allowAccess) {
        this.allowAccess = allowAccess;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public User getActualOrThrow() {
        if (!allowAccess) {
            throw new CustomException(HttpStatus.FORBIDDEN, "Usuario no identificado", "stub");
        }
        return user;
    }

    @Override
    public User valid() {
        return getActualOrThrow();
    }

    @Override
    public User valid(List<Enum<?>> required) {
        if (!allowAccess) {
            throw new CustomException(HttpStatus.FORBIDDEN, "No permitido", "stub");
        }
        return user;
    }

    @Override
    public Company getCurrentCompanyOrThrow() {
        if (company == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Empresa requerida", "stub");
        }
        return company;
    }
}
