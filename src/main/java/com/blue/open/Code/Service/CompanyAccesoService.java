package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.CompanyBloqueoAcceso;
import com.blue.open.Code.Repository.CompanyBloqueoAccesoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Unico punto de lectura/escritura del bloqueo de acceso de un club por facturacion vencida.
 * <p>
 * Lo consulta AccesoBloqueadoInterceptor en cada request. Lo actualiza FacturaService cuando un
 * PAGO_IMPUTADO deja al club sin facturas VENCIDA (desbloqueo inmediato); bloquear() queda listo
 * para el job de vencimiento de facturas, que todavia no existe.
 */
@Service
public class CompanyAccesoService {

    private static final int STATUS_ACTIVE = 0;

    @Autowired
    private CompanyBloqueoAccesoRepository companyBloqueoAccesoRepository;

    @Transactional(readOnly = true)
    public boolean estaBloqueada(Company company) {
        return this.companyBloqueoAccesoRepository.findFirstByCompanyIdAndStatus(company.getId(), STATUS_ACTIVE)
                .map(CompanyBloqueoAcceso::estaBloqueada)
                .orElse(false);
    }

    @Transactional
    public void bloquear(Company company) {
        CompanyBloqueoAcceso estado = this.obtenerOCrear(company);
        if (!estado.estaBloqueada()) {
            estado.bloquear();
            this.companyBloqueoAccesoRepository.save(estado);
        }
    }

    @Transactional
    public void desbloquear(Company company) {
        CompanyBloqueoAcceso estado = this.obtenerOCrear(company);
        if (estado.estaBloqueada()) {
            estado.desbloquear();
            this.companyBloqueoAccesoRepository.save(estado);
        }
    }

    private CompanyBloqueoAcceso obtenerOCrear(Company company) {
        return this.companyBloqueoAccesoRepository.findFirstByCompanyIdAndStatus(company.getId(), STATUS_ACTIVE)
                .orElseGet(() -> CompanyBloqueoAcceso.crearDesbloqueada(company));
    }
}
