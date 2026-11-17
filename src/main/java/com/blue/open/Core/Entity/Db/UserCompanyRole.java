package com.blue.open.Core.Entity.Db;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@Table(name = "auth_users_companies")
@Entity()
public class UserCompanyRole extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "auth_user_company_roles", joinColumns = @JoinColumn(name = "user_company_role_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private List<Role> roles = new java.util.ArrayList<>();
}
