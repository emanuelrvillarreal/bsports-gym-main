package com.blue.open.Crm.Repository.Custom;

import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Crm.Entity.Db.CrmIntegracionLog;
import org.springframework.data.domain.Page;

/**
 * Consultas propias del log de interacciones con el CRM (blue-crm).
 * <p>
 * Aporta la implementacion de paginate, que EntityRepository declara a traves de
 * PaginateAutomatico pero no implementa. Sin este fragmento, Spring Data no puede resolver el
 * metodo y el contexto no levanta.
 */
public interface CrmIntegracionLogRepositoryCustom {

    Page<CrmIntegracionLog> paginate(PaginateList paginateList);
}
