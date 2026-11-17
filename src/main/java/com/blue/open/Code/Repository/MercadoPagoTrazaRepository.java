package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.MercadoPagoTraza;
import com.blue.open.Code.Repository.Custom.MercadoPagoTrazaRepositoryCustom;
import com.blue.open.Core.Repository.EntityRepository;

public interface MercadoPagoTrazaRepository extends EntityRepository<MercadoPagoTraza>,
        MercadoPagoTrazaRepositoryCustom {
}
