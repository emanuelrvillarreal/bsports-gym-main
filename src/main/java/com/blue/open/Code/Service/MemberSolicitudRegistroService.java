package com.blue.open.Code.Service;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.MemberSolicitudPaginateRequestDTO;
import com.blue.open.Code.Entity.DTO.MemberSolicitudRegistroDTO;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.MemberRegistroTipoEnum;
import com.blue.open.Code.Repository.MemberRepository;
import com.blue.open.Code.Repository.MemberSpecifications;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MemberSolicitudRegistroService {

    private static final int MAX_PAGE_SIZE = 100;

    @Autowired
    private AuthService authService;

    @Autowired
    private MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public Page<MemberSolicitudRegistroDTO> paginar(MemberSolicitudPaginateRequestDTO request) {
        authService.valid(List.of(PermissionEnum.MEMBER_VIEW));
        Long companyId = authService.getCurrentCompanyOrThrow().getId();

        MemberSolicitudPaginateRequestDTO req = request != null ? request : new MemberSolicitudPaginateRequestDTO();
        int page = Math.max(req.getPage(), 1);
        int size = req.getSize() <= 0 ? 10 : Math.min(req.getSize(), MAX_PAGE_SIZE);

        Specification<Member> spec = Specification.where(MemberSpecifications.forCompanyPendingApproval(companyId))
                .and(MemberSpecifications.withSearch(req.getSearch()))
                .and(MemberSpecifications.fetchUserAndTitular());

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(
                Sort.Order.desc("dateCreated"),
                Sort.Order.asc("id")));

        return memberRepository.findAll(spec, pageable).map(this::toDto);
    }

    private MemberSolicitudRegistroDTO toDto(Member member) {
        MemberSolicitudRegistroDTO dto = new MemberSolicitudRegistroDTO();
        dto.setMemberUuid(member.getUuid());
        dto.setTipoRegistro(member.isTitular()
                ? MemberRegistroTipoEnum.SOCIO_TITULAR
                : MemberRegistroTipoEnum.FAMILIAR);
        dto.setParentesco(member.getParentesco());
        dto.setNumeroSocio(member.getNumeroSocio());

        if (member.getTitular() != null) {
            dto.setTitularMemberUuid(member.getTitular().getUuid());
            User titularUser = member.getTitular().getUser();
            if (titularUser != null) {
                dto.setTitularNombre(formatNombre(titularUser.getNombre(), titularUser.getApellido()));
            }
        }

        User user = member.getUser();
        if (user != null) {
            dto.setNombre(user.getNombre());
            dto.setApellido(user.getApellido());
            dto.setDni(user.getDni());
            dto.setFechaNacimiento(user.getFechaNacimiento());
            dto.setSexo(user.getSexo());
            dto.setTelefono(user.getPhone());
            dto.setEmail(user.getEmail());
        }

        if (member.getDateCreated() != null) {
            dto.setFechaSolicitud(member.getDateCreated().toLocalDateTime());
        }
        return dto;
    }

    private String formatNombre(String nombre, String apellido) {
        String n = nombre != null ? nombre.trim() : "";
        String a = apellido != null ? apellido.trim() : "";
        return (n + " " + a).trim();
    }
}
