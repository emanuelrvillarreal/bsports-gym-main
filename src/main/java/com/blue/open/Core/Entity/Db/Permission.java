package com.blue.open.Core.Entity.Db;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Table(name = "auth_permissions")
@Entity()
public class Permission extends AbstractEntity {
    protected String name;
    protected String description;
}
