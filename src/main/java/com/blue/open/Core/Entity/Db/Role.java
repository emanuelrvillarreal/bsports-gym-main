package com.blue.open.Core.Entity.Db;

import com.blue.open.Code.Entity.Db.Company;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Table(name = "auth_roles")
@Entity()
public class Role extends AbstractEntity {
    protected String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    protected Company company;

    @Column(name = "is_default", columnDefinition = "TINYINT DEFAULT 0")
    @JdbcTypeCode(Types.TINYINT)
    protected Integer isDefault = 0;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "auth_roles_permissions", joinColumns = @JoinColumn(name = "role_id"), inverseJoinColumns = @JoinColumn(name = "permission_id"))
    protected List<com.blue.open.Core.Entity.Db.Permission> permissions = new ArrayList<>();
}
