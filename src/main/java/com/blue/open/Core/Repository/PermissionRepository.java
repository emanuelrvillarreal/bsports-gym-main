package com.blue.open.Core.Repository;

import com.blue.open.Core.Entity.Db.Permission;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionRepository extends EntityRepository<Permission> {
    Permission findFirstByName(String name);
}
