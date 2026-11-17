package com.blue.open.Code.Repository.Custom;

import com.blue.open.Code.Entity.Db.CodigoLogin;
import com.blue.open.Core.Entity.DTO.PaginateList;
import org.springframework.data.domain.Page;

/**
 * Aporta la implementacion de paginate, que EntityRepository declara a traves de
 * PaginateAutomatico pero no implementa. Sin este fragmento, Spring Data no puede resolver el
 * metodo y el contexto no levanta.
 */
public interface CodigoLoginRepositoryCustom {

    Page<CodigoLogin> paginate(PaginateList paginateList);
}
