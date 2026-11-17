package com.blue.open.Code.Service.Amenities;

import com.blue.open.Code.Entity.DTO.PortalReservaPaginateRequestDTO;
import com.blue.open.Code.Entity.DTO.ReservaAmenityDTO;
import com.blue.open.Code.Entity.DTO.ReservaPaginateRequestDTO;
import com.blue.open.Code.Entity.Db.Reserva;
import com.blue.open.Code.Mapper.AmenityMapper;
import com.blue.open.Code.Repository.ReservaRepository;
import com.blue.open.Code.Repository.ReservaSpecifications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservaQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private AmenityMapper amenityMapper;

    @Transactional(readOnly = true)
    public Page<ReservaAmenityDTO> paginar(ReservaPaginateRequestDTO request, Long companyId) {
        ReservaPaginateRequestDTO req = request != null ? request : new ReservaPaginateRequestDTO();
        int page = Math.max(req.getPage(), 1);
        int size = req.getSize() <= 0 ? 10 : Math.min(req.getSize(), MAX_PAGE_SIZE);

        Specification<Reserva> spec = Specification.where(ReservaSpecifications.forCompany(companyId))
                .and(ReservaSpecifications.withPaginateRequest(req))
                .and(ReservaSpecifications.fetchListDetails());

        Pageable pageable = PageRequest.of(page - 1, size,
                Sort.by(Sort.Order.desc("dateCreated"), Sort.Order.desc("dateUpdated")));

        return reservaRepository.findAll(spec, pageable).map(amenityMapper::reservaToDto);
    }

    @Transactional(readOnly = true)
    public Page<ReservaAmenityDTO> paginarPorMember(
            PortalReservaPaginateRequestDTO request, Long companyId, Long memberId) {
        PortalReservaPaginateRequestDTO req = request != null ? request : new PortalReservaPaginateRequestDTO();
        int page = Math.max(req.getPage(), 1);
        int size = req.getSize() <= 0 ? 10 : Math.min(req.getSize(), MAX_PAGE_SIZE);

        Pageable pageable = PageRequest.of(page - 1, size);

        return reservaRepository.paginateByCompanyIdAndMemberId(companyId, memberId, pageable)
                .map(amenityMapper::reservaToDto);
    }
}
