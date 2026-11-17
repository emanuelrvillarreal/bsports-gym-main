package com.blue.open.Code.Service;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.MemberDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Mapper.MemberMapper;
import com.blue.open.Code.Repository.MemberRepository;
import com.blue.open.Core.Entity.DTO.DeleteArchivoRequest;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Repository.UserRepository;
import com.blue.open.Core.Service.Auth.AuthService;
import com.blue.open.Core.Service.SM3Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Permite que el administrador cambie o quite la foto de perfil de un socio (titular o familiar) desde el backoffice.
 * La foto es la del usuario del socio: es la misma que ve el socio en su credencial.
 */
@Service
public class MemberFotoService {

    @Autowired
    private AuthService authService;

    @Autowired
    private MemberAuthService memberAuthService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private SM3Service sm3Service;

    @Transactional
    public MemberDTO subir(String memberUuid, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Archivo requerido",
                    "Elegí una imagen para la foto del socio.");
        }
        String tipo = file.getContentType();
        if (tipo == null || !tipo.toLowerCase().startsWith("image/")) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Archivo no válido",
                    "La foto tiene que ser una imagen (JPG, PNG o WEBP).");
        }
        Member member = resolver(memberUuid);
        User user = member.getUser();
        borrarDelStorage(user.getImageUrl());
        user.setImageUrl(sm3Service.upload(file));
        userRepository.save(user);
        return dto(member);
    }

    @Transactional
    public MemberDTO eliminar(String memberUuid) {
        Member member = resolver(memberUuid);
        User user = member.getUser();
        borrarDelStorage(user.getImageUrl());
        user.setImageUrl(null);
        userRepository.save(user);
        return dto(member);
    }

    private Member resolver(String memberUuid) {
        Company company = authService.getCurrentCompanyOrThrow();
        Member member = memberAuthService.resolveMemberByUuid(memberUuid, company.getId());
        authService.valid(List.of(member.isTitular() ? PermissionEnum.MEMBER_EDIT : PermissionEnum.FAMILY_EDIT));
        return member;
    }

    private MemberDTO dto(Member member) {
        return memberMapper.modelToDto(memberRepository.findByIdWithTitular(member.getId()).orElse(member));
    }

    private void borrarDelStorage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return;
        DeleteArchivoRequest request = new DeleteArchivoRequest();
        request.setUrl(imageUrl);
        try {
            sm3Service.delete(request);
        } catch (RuntimeException ignored) {
            // Si la foto vieja ya no existe en el servidor de imágenes, igual se reemplaza.
        }
    }
}
