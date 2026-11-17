package com.blue.open.Code.Service.Descuento;

import com.blue.open.Code.Entity.DTO.DescuentoDTO;
import com.blue.open.Code.Entity.DTO.DescuentoEdicionCommand;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Descuento;
import com.blue.open.Code.Entity.Enums.DescuentoTipoValorEnum;
import com.blue.open.Code.Mapper.DescuentoMapper;
import com.blue.open.Code.Repository.DescuentoRepository;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DescuentoEdicionService {

    @Autowired
    private AuthService authService;

    @Autowired
    private DescuentoRepository descuentoRepository;

    @Autowired
    private DescuentoMapper descuentoMapper;

    @Transactional
    public DescuentoDTO editar(String descuentoUuid, DescuentoEdicionCommand command) {
        Company company = authService.getCurrentCompanyOrThrow();

        Descuento descuento = descuentoRepository.findByUuidAndCompanyIdAndStatusActive(descuentoUuid, company.getId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Descuento no encontrado",
                        "DescuentoEdicionService | descuentoUuid: " + descuentoUuid));

        if (command.getTipoValor() == DescuentoTipoValorEnum.PORCENTAJE) {
            descuento.editarComoPorcentaje(command.getTitulo(), command.getPorcentaje());
        } else {
            descuento.editarComoMontoFijo(command.getTitulo(), command.getMontoFijo());
        }

        descuento = descuentoRepository.save(descuento);

        return descuentoMapper.toDto(descuento);
    }
}
