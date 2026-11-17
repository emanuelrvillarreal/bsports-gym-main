package com.blue.open.Code.Service.Amenities;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.AmenityEdicionCommand;
import com.blue.open.Code.Entity.DTO.AmenityDTO;
import com.blue.open.Code.Entity.Db.Amenity;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Mapper.AmenityMapper;
import com.blue.open.Code.Repository.AmenityRepository;
import com.blue.open.Code.Util.DiasSemanaUtil;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AmenityEdicionService {

    @Autowired
    private AuthService authService;

    @Autowired
    private AmenityRepository amenityRepository;

    @Autowired
    private AmenityMapper amenityMapper;

    @Transactional
    public AmenityDTO editar(String uuid, AmenityEdicionCommand command) {
        authService.valid(List.of(PermissionEnum.AMENITY_EDIT));
        Company company = authService.getCurrentCompanyOrThrow();

        Amenity amenity = amenityRepository.findByUuidAndCompanyIdAndStatusActive(uuid, company.getId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Amenity no encontrado",
                        "AmenityEdicionService | uuid: " + uuid));

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
