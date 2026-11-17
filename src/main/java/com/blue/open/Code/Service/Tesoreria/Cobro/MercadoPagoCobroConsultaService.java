package com.blue.open.Code.Service.Tesoreria.Cobro;

import com.blue.open.Code.Entity.DTO.PagoDetalleDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.MercadoPagoCobro;
import com.blue.open.Code.Mapper.PagoMapper;
import com.blue.open.Code.Repository.MercadoPagoCobroRepository;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MercadoPagoCobroConsultaService {

    private final AuthService authService;
    private final MercadoPagoCobroRepository mercadoPagoCobroRepository;
    private final PagoMapper pagoMapper;

    @Transactional(readOnly = true)
    public PagoDetalleDTO obtenerPorExternalReference(String externalReference) {
        if (externalReference == null || externalReference.isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Referencia de pago requerida",
                    "MercadoPagoCobroConsultaService | externalReference vacío");
        }

        Company company = authService.getCurrentCompanyOrThrow();

        MercadoPagoCobro cobro = mercadoPagoCobroRepository
                .findActiveByIntentExternalReferenceAndCompanyId(externalReference.trim(), company.getId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Pago no encontrado",
                        "MercadoPagoCobroConsultaService | externalReference: " + externalReference));

        return pagoMapper.toDetalleDto(cobro);
    }
}
