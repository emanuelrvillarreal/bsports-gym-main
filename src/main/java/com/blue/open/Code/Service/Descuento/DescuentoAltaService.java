package com.blue.open.Code.Service.Descuento;

import com.blue.open.Code.Entity.DTO.DescuentoAltaCommand;
import com.blue.open.Code.Entity.DTO.DescuentoDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Descuento;
import com.blue.open.Code.Entity.Enums.DescuentoTipoValorEnum;
import com.blue.open.Code.Mapper.DescuentoMapper;
import com.blue.open.Code.Repository.DescuentoRepository;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DescuentoAltaService {

    @Autowired
    private AuthService authService;

    @Autowired
    private DescuentoRepository descuentoRepository;

    @Autowired
    private DescuentoMapper descuentoMapper;

    @Transactional
    public DescuentoDTO alta(DescuentoAltaCommand command) {
        Company company = authService.getCurrentCompanyOrThrow();

        Descuento descuento = command.getTipoValor() == DescuentoTipoValorEnum.PORCENTAJE
                ? Descuento.dePorcentaje(company, command.getTitulo(), command.getPorcentaje())
                : Descuento.deMontoFijo(company, command.getTitulo(), command.getMontoFijo());

        descuento = descuentoRepository.save(descuento);

        return descuentoMapper.toDto(descuento);
    }
}
