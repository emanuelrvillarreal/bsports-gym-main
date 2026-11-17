package com.blue.open.Code.Service.Amenities;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.AmenityDTO;
import com.blue.open.Code.Mapper.AmenityMapper;
import com.blue.open.Code.Repository.AmenityRepository;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AmenityListadoService {

    @Autowired
    private AuthService authService;

    @Autowired
    private AmenityRepository amenityRepository;

    @Autowired
    private AmenityMapper amenityMapper;

    @Transactional(readOnly = true)
    public List<AmenityDTO> listarActivasStaff() {
        authService.valid(List.of(PermissionEnum.AMENITY_VIEW));
        Long companyId = authService.getCurrentCompanyOrThrow().getId();
        return amenityRepository.findActivasByCompanyId(companyId).stream()
                .map(amenityMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AmenityDTO> listarActivasPortal(Long companyId) {
        return amenityRepository.findActivasByCompanyId(companyId).stream()
                .map(amenityMapper::toDto)
                .toList();
    }
}
