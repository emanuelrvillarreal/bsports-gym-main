package com.blue.open.Code.Service.CuotaSocialRango;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.CuotaSocialRango;
import com.blue.open.Code.Repository.CuotaSocialRangoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Garantiza que el club tenga su rango catch-all (es_default) activo, igual que la migración v72. */
@Service
public class CuotaSocialRangoDefaultService {

    @Autowired
    private CuotaSocialRangoRepository cuotaSocialRangoRepository;

    @Transactional
    public void asegurarDefault(Company company) {
        if (cuotaSocialRangoRepository.existsDefaultActivoEnCompany(company.getId())) {
            return;
        }

        cuotaSocialRangoRepository.save(CuotaSocialRango.crearDefault(company));
    }
}
