package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.DTO.CertificadoMedicoDTO;
import com.blue.open.Code.Entity.DTO.CertificadoMedicoEstadoUpdateCommand;
import com.blue.open.Code.Entity.Db.CertificadoMedico;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Mapper.CertificadoMedicoMapper;
import com.blue.open.Code.Repository.CertificadoMedicoRepository;
import com.blue.open.Code.Repository.MemberRepository;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CertificadoMedicoEstadoUpdateService {

    @Autowired
    private AuthService authService;

    @Autowired
    private CertificadoMedicoRepository certificadoMedicoRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CertificadoMedicoMapper certificadoMedicoMapper;

    @Transactional
    public CertificadoMedicoDTO actualizarEstado(String certificadoUuid, CertificadoMedicoEstadoUpdateCommand command) {
        Company company = authService.getCurrentCompanyOrThrow();

        CertificadoMedico certificado = certificadoMedicoRepository.findFirstByUuidWithStatusIsActive(certificadoUuid);
        if (certificado == null) {
            throw new CustomException(HttpStatus.NOT_FOUND, "Certificado no encontrado",
                    "CertificadoMedicoEstadoUpdateService | uuid: " + certificadoUuid);
        }

        Member member = memberRepository.findByUserIdAndCompanyIdWithDetails(certificado.getUser().getId(), company.getId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Certificado no encontrado",
                        "CertificadoMedicoEstadoUpdateService | uuid: " + certificadoUuid + " companyId: " + company.getId()));

        certificado.setEstado(command.getEstado());
        certificado = certificadoMedicoRepository.save(certificado);

        return certificadoMedicoMapper.modelToDto(certificado, member.getUuid());
    }
}
