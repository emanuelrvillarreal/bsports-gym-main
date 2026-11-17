package com.blue.open.Core.Repository;

import com.blue.open.Core.Entity.Db.AbstractEntity;
import com.blue.open.Core.Repository.Custom.PaginateAutomatico;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

@NoRepositoryBean
public interface EntityRepository<T extends AbstractEntity> extends CrudRepository<T, Long>, JpaSpecificationExecutor<T>, PaginateAutomatico<T> {
    @Query("SELECT e FROM #{#entityName} e WHERE e.uuid = :uuid AND e.status = 0")
    T findFirstByUuidWithStatusIsActive(@Param("uuid") String uuid);

    @Query("SELECT e FROM #{#entityName} e WHERE e.id = :id AND e.status = 0")
    T findFirstByIdWithStatusIsActive(@Param("id") Long id);

    T findFirstByUuid(@Param("uuid") String uuid);

    T findFirstById(@Param("id") Long id);
}
