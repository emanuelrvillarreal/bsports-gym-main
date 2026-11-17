package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.DTO.MemberAltaUserFields;
import com.blue.open.Code.Entity.DTO.MemberDTO;
import com.blue.open.Code.Entity.DTO.MemberFamiliarAltaCommand;
import com.blue.open.Code.Entity.DTO.MemberTitularAltaCommand;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Enums.ParentescoEnum;
import com.blue.open.Code.Repository.MemberRepository;
import com.blue.open.Core.Exceptions.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

@Service
public class MemberBulkImportRowProcessor {

    @Autowired
    private MemberTitularService memberTitularService;

    @Autowired
    private MemberFamiliarService memberFamiliarService;

    @Autowired
    private MemberRepository memberRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProcessedRow processTitular(Company company, MemberTitularAltaCommand command) {
        MemberDTO dto = memberTitularService.registrarTitularMigracionEnClub(company, command);
        Member member = memberRepository.findByUuidAndCompanyIdWithDetails(dto.getMemberUuid(), company.getId())
                .orElseThrow(() -> new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al cargar socio",
                        "MemberBulkImportRowProcessor | memberUuid: " + dto.getMemberUuid()));
        return new ProcessedRow(dto, member);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProcessedRow processFamiliar(Company company, Member titular, MemberFamiliarAltaCommand command) {
        MemberDTO dto = memberFamiliarService.registrarFamiliarMigracionEnClub(company, titular, command);
        return new ProcessedRow(dto, null);
    }

    public record ProcessedRow(MemberDTO memberDto, Member titularMember) {
    }

    public enum RowKind {
        TITULAR,
        FAMILIAR
    }

    public RowKind resolveRowKind(MemberBulkImportRow row) {
        boolean hasTitularRef = row.getTitularDni() != null && !row.getTitularDni().isBlank();
        boolean hasParentesco = row.getParentesco() != null;
        if (!hasTitularRef && !hasParentesco) {
            return RowKind.TITULAR;
        }
        if (hasTitularRef && hasParentesco) {
            return RowKind.FAMILIAR;
        }
        return null;
    }

    public MemberTitularAltaCommand toTitularCommand(MemberBulkImportRow row) {
        MemberTitularAltaCommand command = new MemberTitularAltaCommand();
        fillUserFields(command, row);
        command.setNumeroSocio(row.getNumeroSocio());
        command.setFechaAlta(row.getFechaAlta() != null ? row.getFechaAlta() : LocalDate.now());
        return command;
    }

    public MemberFamiliarAltaCommand toFamiliarCommand(MemberBulkImportRow row, ParentescoEnum parentesco) {
        MemberFamiliarAltaCommand command = new MemberFamiliarAltaCommand();
        fillUserFields(command, row);
        command.setNumeroSocio(row.getNumeroSocio());
        command.setParentesco(parentesco);
        command.setFechaAlta(row.getFechaAlta() != null ? row.getFechaAlta() : LocalDate.now());
        return command;
    }

    private void fillUserFields(MemberAltaUserFields command, MemberBulkImportRow row) {
        command.setNombre(row.getNombre());
        command.setApellido(row.getApellido());
        command.setDni(row.getDni());
        command.setEmail(row.getEmail());
        command.setPassword(row.getPassword());
        command.setTelefono(row.getTelefono());
        command.setFechaNacimiento(row.getFechaNacimiento());
        command.setSexo(row.getSexo());
    }

    public void registerTitularInCache(Member member, Map<String, Member> titularCache) {
        if (member == null || member.getUser() == null) {
            return;
        }
        if (member.getUser().getDni() != null && !member.getUser().getDni().isBlank()) {
            titularCache.put(cacheKey(member.getUser().getDni()), member);
        }
        if (member.getUser().getEmail() != null && !member.getUser().getEmail().isBlank()) {
            titularCache.put(cacheKey(member.getUser().getEmail()), member);
        }
    }

    public Member resolveTitular(Company company, String identifier, Map<String, Member> titularCache) {
        if (identifier == null || identifier.isBlank()) {
            return null;
        }
        Member cached = titularCache.get(cacheKey(identifier));
        if (cached != null) {
            return cached;
        }
        return memberRepository.findTitularByUserDniOrEmailAndCompanyId(company.getId(), identifier.trim())
                .orElse(null);
    }

    public static String cacheKey(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
