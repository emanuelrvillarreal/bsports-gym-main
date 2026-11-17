package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Enums.MemberRegistroTipoEnum;
import com.blue.open.Code.Notifications.MemberRegistroPendienteMail;
import com.blue.open.Code.Repository.MemberRepository;
import com.blue.open.Core.Service.JavaMailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MemberRegistroNotificacionService {

    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    private MemberRepository memberRepository;

    public void notificarSolicitudPendiente(Company company, Member member, MemberRegistroTipoEnum tipoRegistro) {
        if (company == null || member == null) {
            return;
        }
        String destino = company.getEmailInstitucional();
        if (destino == null || destino.isBlank()) {
            log.warn("MemberRegistroNotificacionService | email institucional vacío para companyId={}",
                    company.getId());
            return;
        }

        Member memberWithDetails = memberRepository.findByIdWithTitular(member.getId()).orElse(member);
        MemberRegistroPendienteMail mail = new MemberRegistroPendienteMail(company, memberWithDetails, tipoRegistro);

        try {
            javaMailSender.sendEmail(destino.trim(), mail.getSubject(), mail.getBody());
        } catch (Exception e) {
            log.error("MemberRegistroNotificacionService | error enviando mail a {}: {}",
                    destino, e.getMessage());
        }
    }
}
