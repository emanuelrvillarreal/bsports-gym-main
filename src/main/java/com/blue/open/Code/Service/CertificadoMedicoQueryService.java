package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.DTO.CertificadoMedicoDTO;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Mapper.CertificadoMedicoMapper;
import com.blue.open.Code.Repository.CertificadoMedicoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CertificadoMedicoQueryService {

    private static final int STATUS_ACTIVE = 0;

    @Autowired
    private MemberAuthService memberAuthService;

    @Autowired
    private CertificadoMedicoRepository certificadoMedicoRepository;

    @Autowired
    private CertificadoMedicoMapper certificadoMedicoMapper;

    @Transactional(readOnly = true)
    public List<CertificadoMedicoDTO> listar(String memberUuid, Long companyId) {
        Member member = memberAuthService.resolveMemberByUuid(memberUuid, companyId);
        return listarPorMember(member);
    }

    @Transactional(readOnly = true)
    public List<CertificadoMedicoDTO> listarPorMember(Member member) {
        return certificadoMedicoRepository.findByUser_IdAndStatus(member.getUser().getId(), STATUS_ACTIVE).stream()
                .map(certificado -> certificadoMedicoMapper.modelToDto(certificado, member.getUuid()))
                .toList();
    }
}
