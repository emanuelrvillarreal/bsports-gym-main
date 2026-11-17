package com.blue.open.Core.Entity.Db;

import jakarta.persistence.CascadeType;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@MappedSuperclass
public abstract class BaseCompany extends AbstractEntity {

    private String name, path, description, code;
    private Double balance;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserCompanyRole> userCompanyRoles = new ArrayList<>();
}
