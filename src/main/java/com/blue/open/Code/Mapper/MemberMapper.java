package com.blue.open.Code.Mapper;

import com.blue.open.Code.Entity.DTO.MemberAltaUserFields;
import com.blue.open.Code.Entity.DTO.MemberBackofficeUpdateCommand;
import com.blue.open.Code.Entity.DTO.MemberCuotaSocialRangoDTO;
import com.blue.open.Code.Entity.DTO.MemberDTO;
import com.blue.open.Code.Entity.DTO.MemberPortalProfileCommand;
import com.blue.open.Code.Entity.Db.CuotaSocialRango;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MemberMapper {

    private static final int STATUS_ACTIVE = 0;

    @Autowired
    private MemberRepository memberRepository;

    public MemberDTO modelToDto(Member member) {
        if (member == null) {
            return null;
        }
        MemberDTO dto = new MemberDTO();
        dto.setMemberUuid(member.getUuid());
        dto.setTitular(member.isTitular());
        dto.setTieneFamiliares(
                member.isTitular() && memberRepository.existsByTitular_IdAndStatus(member.getId(), STATUS_ACTIVE));
        if (member.getTitular() != null) {
            dto.setTitularMemberUuid(member.getTitular().getUuid());
        }
        dto.setParentesco(member.getParentesco());
        dto.setNumeroSocio(member.getNumeroSocio());
        dto.setEstado(member.getEstado());
        dto.setStatus(member.getStatus());
        dto.setFechaAlta(member.getFechaAlta());
        dto.setCuotaSocialRango(cuotaSocialRangoToDto(member.getCuotaSocialRango()));

        User user = member.getUser();
        if (user != null) {
            dto.setUserUuid(user.getUuid());
            dto.setNombre(user.getNombre());
            dto.setApellido(user.getApellido());
            dto.setDni(user.getDni());
            dto.setFechaNacimiento(user.getFechaNacimiento());
            dto.setSexo(user.getSexo());
            dto.setTelefono(user.getPhone());
            dto.setEmail(user.getEmail());
            dto.setImageUrl(user.getImageUrl());
        }
        return dto;
    }

    private MemberCuotaSocialRangoDTO cuotaSocialRangoToDto(CuotaSocialRango rango) {
        if (rango == null) {
            return null;
        }
        MemberCuotaSocialRangoDTO dto = new MemberCuotaSocialRangoDTO();
        dto.setUuid(rango.getUuid());
        dto.setNombre(rango.getNombre());
        return dto;
    }

    public void applyAltaUserFields(MemberAltaUserFields fields, User user) {
        user.setNombre(trimOrNull(fields.getNombre()));
        user.setApellido(trimOrNull(fields.getApellido()));
        user.setDni(trimOrNull(fields.getDni()));
        user.setFechaNacimiento(fields.getFechaNacimiento());
        user.setSexo(fields.getSexo());
        if (fields.getTelefono() != null) {
            user.setPhone(fields.getTelefono().trim());
        }
        String email = fields.getEmail().trim();
        user.setEmail(email);
        user.setUsername(email);
        syncDisplayName(user);
    }

    public void applyBackofficeUpdate(MemberBackofficeUpdateCommand command, Member member) {
        User user = member.getUser();
        if (command.getNombre() != null) {
            user.setNombre(trimOrNull(command.getNombre()));
        }
        if (command.getApellido() != null) {
            user.setApellido(trimOrNull(command.getApellido()));
        }
        if (command.getDni() != null) {
            user.setDni(trimOrNull(command.getDni()));
        }
        if (command.getFechaNacimiento() != null) {
            user.setFechaNacimiento(command.getFechaNacimiento());
        }
        if (command.getSexo() != null) {
            user.setSexo(command.getSexo());
        }
        if (command.getTelefono() != null) {
            user.setPhone(command.getTelefono().trim());
        }
        if (command.getEmail() != null && !command.getEmail().isBlank()) {
            String email = command.getEmail().trim();
            user.setEmail(email);
            user.setUsername(email);
        }
        syncDisplayName(user);
        if (!member.isTitular() && command.getParentesco() != null) {
            member.setParentesco(command.getParentesco());
        }
    }

    public void applyPortalProfileUpdate(MemberPortalProfileCommand command, Member member) {
        User user = member.getUser();
        if (command.getNombre() != null) {
            user.setNombre(trimOrNull(command.getNombre()));
        }
        if (command.getApellido() != null) {
            user.setApellido(trimOrNull(command.getApellido()));
        }
        if (command.getFechaNacimiento() != null) {
            user.setFechaNacimiento(command.getFechaNacimiento());
        }
        if (command.getSexo() != null) {
            user.setSexo(command.getSexo());
        }
        if (command.getTelefono() != null) {
            user.setPhone(command.getTelefono().trim());
        }
        syncDisplayName(user);
    }

    private void syncDisplayName(User user) {
        String nombre = user.getNombre();
        String apellido = user.getApellido();
        if ((nombre == null || nombre.isBlank()) && (apellido == null || apellido.isBlank())) {
            return;
        }
        StringBuilder fullName = new StringBuilder();
        if (nombre != null && !nombre.isBlank()) {
            fullName.append(nombre.trim());
        }
        if (apellido != null && !apellido.isBlank()) {
            if (fullName.length() > 0) {
                fullName.append(' ');
            }
            fullName.append(apellido.trim());
        }
        user.setName(fullName.toString());
    }

    private String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
