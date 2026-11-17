package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.MercadoPagoCobroItem;
import com.blue.open.Code.Repository.Custom.MercadoPagoCobroItemRepositoryCustom;
import com.blue.open.Core.Repository.EntityRepository;

public interface MercadoPagoCobroItemRepository extends EntityRepository<MercadoPagoCobroItem>,
        MercadoPagoCobroItemRepositoryCustom {
}
