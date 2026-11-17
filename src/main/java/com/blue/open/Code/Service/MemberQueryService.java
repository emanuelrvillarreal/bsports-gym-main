package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.DTO.MemberDTO;
import com.blue.open.Code.Entity.DTO.MemberPaginateRequestDTO;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Mapper.MemberMapper;
import com.blue.open.Code.Repository.MemberRepository;
import com.blue.open.Code.Repository.MemberSpecifications;
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
public class MemberQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private MemberAuthService memberAuthService;

    @Transactional(readOnly = true)
    public Page<MemberDTO> paginar(MemberPaginateRequestDTO request, Long companyId) {
        MemberPaginateRequestDTO req = request != null ? request : new MemberPaginateRequestDTO();
        int page = Math.max(req.getPage(), 1);
        int size = req.getSize() <= 0 ? 10 : Math.min(req.getSize(), MAX_PAGE_SIZE);

        Specification<Member> spec = Specification.where(MemberSpecifications.forCompany(companyId))
                .and(MemberSpecifications.withPaginateRequest(req))
                .and(MemberSpecifications.fetchUserAndTitular());

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(
                Sort.Order.asc("user.apellido"),
                Sort.Order.asc("user.nombre"),
                Sort.Order.asc("id")));

        return memberRepository.findAll(spec, pageable).map(memberMapper::modelToDto);
    }

    @Transactional(readOnly = true)
    public List<MemberDTO> listarFamiliares(String memberUuid, Long companyId) {
        Member titular = memberAuthService.resolveTitularByUuid(memberUuid, companyId);
        return memberRepository.findGrupoByTitularId(companyId, titular.getId()).stream()
                .filter(m -> !m.getId().equals(titular.getId()))
                .map(memberMapper::modelToDto)
                .toList();
    }
}
