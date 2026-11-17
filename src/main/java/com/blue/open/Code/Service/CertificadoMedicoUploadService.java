package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.DTO.CertificadoMedicoDTO;
import com.blue.open.Code.Entity.Db.CertificadoMedico;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Enums.CertificadoMedicoEstadoEnum;
import com.blue.open.Code.Mapper.CertificadoMedicoMapper;
import com.blue.open.Code.Repository.CertificadoMedicoRepository;
import com.blue.open.Core.Service.Auth.AuthService;
import com.blue.open.Core.Service.SM3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Service
public class CertificadoMedicoUploadService {

    @Autowired
    private AuthService authService;

    @Autowired
    private MemberAuthService memberAuthService;

    @Autowired
    private CertificadoMedicoFileValidator fileValidator;

    @Autowired
    private SM3Service sm3Service;

    @Autowired
    private CertificadoMedicoRepository certificadoMedicoRepository;

    @Autowired
    private CertificadoMedicoMapper certificadoMedicoMapper;

    @Transactional
    public CertificadoMedicoDTO subir(String memberUuid, MultipartFile file, boolean esBackoffice) {
        Company company = authService.getCurrentCompanyOrThrow();

        Member member;
        if (esBackoffice) {
            member = memberAuthService.resolveMemberByUuid(memberUuid, company.getId());
        } else {
            member = memberAuthService.resolveMemberOfFamilyOrThrow(memberUuid, company.getId());
            memberAuthService.requirePortalEligible(member);
        }

        fileValidator.validar(file);

        String url = sm3Service.upload(file);
        LocalDate fechaCarga = LocalDate.now();
        LocalDate fechaExpiracion = company.calcularVencimientoCertificado(fechaCarga);

        CertificadoMedico certificado = new CertificadoMedico();
        certificado.setUser(member.getUser());
        certificado.setFechaCarga(fechaCarga);
        certificado.setFechaExpiracion(fechaExpiracion);
        certificado.setNombreArchivo(file.getOriginalFilename());
        certificado.setUrlArchivo(url);
        certificado.setEstado(esBackoffice
                ? CertificadoMedicoEstadoEnum.CONFIRMADO
                : CertificadoMedicoEstadoEnum.PENDIENTE_VALIDACION);

        certificado = certificadoMedicoRepository.save(certificado);
        return certificadoMedicoMapper.modelToDto(certificado, member.getUuid());
    }
}
