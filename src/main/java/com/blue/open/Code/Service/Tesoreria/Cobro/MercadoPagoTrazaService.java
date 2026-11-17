package com.blue.open.Code.Service.Tesoreria.Cobro;

import com.blue.open.Code.Entity.DTO.MercadoPagoTrazaRegistro;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.MercadoPagoCobroIntent;
import com.blue.open.Code.Entity.Db.MercadoPagoTraza;
import com.blue.open.Code.Entity.Enums.MercadoPagoTrazaNivelEnum;
import com.blue.open.Code.Repository.MercadoPagoTrazaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MercadoPagoTrazaService {

    private final MercadoPagoTrazaRepository mercadoPagoTrazaRepository;

    public MercadoPagoTrazaService(MercadoPagoTrazaRepository mercadoPagoTrazaRepository) {
        this.mercadoPagoTrazaRepository = mercadoPagoTrazaRepository;
    }

    public void registrar(MercadoPagoTrazaRegistro registro) {
        if (registro == null || registro.getCompany() == null) {
            log.warn("MP traza omitida: company requerida | etapa={}",
                    registro != null ? registro.getEtapa() : null);
            return;
        }

        escribirSlf4j(registro);

        try {
            this.persistir(registro);
        } catch (Exception ex) {
            log.error("MP traza no persistida | etapa={} | companyId={}",
                    registro.getEtapa(), registro.getCompany().getId(), ex);
        }
    }

    public void persistir(MercadoPagoTrazaRegistro registro) {
        MercadoPagoTraza traza = new MercadoPagoTraza();
        traza.setCompany(registro.getCompany());
        traza.setIntent(registro.getIntent());
        traza.setEtapa(registro.getEtapa());
        traza.setNivel(registro.getNivel() != null ? registro.getNivel() : MercadoPagoTrazaNivelEnum.INFO);
        traza.setExternalReference(registro.getExternalReference());
        traza.setPreferenceId(registro.getPreferenceId());
        traza.setPaymentId(registro.getPaymentId());
        traza.setMensaje(truncar(registro.getMensaje(), 512));
        traza.setRequestRaw(registro.getRequestRaw());
        traza.setResponseRaw(registro.getResponseRaw());
        mercadoPagoTrazaRepository.save(traza);
    }

    private void escribirSlf4j(MercadoPagoTrazaRegistro registro) {
        Company company = registro.getCompany();
        MercadoPagoCobroIntent intent = registro.getIntent();
        String line = "MP traza | companyId={} | etapa={} | nivel={} | externalReference={} | preferenceId={} | paymentId={} | intentUuid={} | mensaje={}";

        Object[] args = {
                company.getId(),
                registro.getEtapa(),
                registro.getNivel(),
                registro.getExternalReference(),
                registro.getPreferenceId(),
                registro.getPaymentId(),
                intent != null ? intent.getUuid() : null,
                registro.getMensaje()
        };

        MercadoPagoTrazaNivelEnum nivel = registro.getNivel() != null ? registro.getNivel() : MercadoPagoTrazaNivelEnum.INFO;
        switch (nivel) {
            case ERROR -> log.error(line, args);
            case WARN -> log.warn(line, args);
            default -> log.info(line, args);
        }
    }

    private String truncar(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
