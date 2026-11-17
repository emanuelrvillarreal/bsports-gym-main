package com.blue.open.Code.Service.Amenities;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.AmenityAltaCommand;
import com.blue.open.Code.Entity.DTO.AmenityDTO;
import com.blue.open.Code.Entity.Db.Amenity;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Mapper.AmenityMapper;
import com.blue.open.Code.Repository.AmenityRepository;
import com.blue.open.Code.Util.DiasSemanaUtil;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AmenityAltaService {

    @Autowired
    private AuthService authService;

    @Autowired
    private AmenityRepository amenityRepository;

    @Autowired
    private AmenityMapper amenityMapper;

    @Transactional
    public AmenityDTO alta(AmenityAltaCommand command) {
        authService.valid(List.of(PermissionEnum.AMENITY_CREATE));
        Company company = authService.getCurrentCompanyOrThrow();

        Amenity amenity = new Amenity();
        amenity.setCompany(company);
        amenity.setNombre(command.getNombre().trim());
        amenity.setMontoHora(command.getMontoHora());
        amenity.setPorcentajeSena(command.getPorcentajeSena());
        amenity.setDiasSemana(DiasSemanaUtil.normalizeStorage(command.getDiasSemana()));
        amenity.setHoraInicio(command.getHoraInicio());
        amenity.setHoraFin(command.getHoraFin());

        amenity = amenityRepository.save(amenity);
        return amenityMapper.toDto(amenity);
    }
}
