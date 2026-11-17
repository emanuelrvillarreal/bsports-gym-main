package com.blue.open.Code.Service;

import com.blue.open.Code.Auth.RoleEnum;
import com.blue.open.Code.Entity.DTO.UserDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Notifications.ForgotPasswordMail;
import com.blue.open.Code.Notifications.RegistrationMail;
import com.blue.open.Code.Repository.CompanyRepository;
import com.blue.open.Core.Entity.Db.Role;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Notifications.MailNotification;
import com.blue.open.Core.Service.BaseUserService;
import com.blue.open.Code.Config.FrontUrlProperties;
import com.blue.open.Code.Entity.DTO.CompanyDTO;
import com.blue.open.Core.Entity.DTO.DeleteArchivoRequest;
import com.blue.open.Core.Repository.UserRepository;
import com.blue.open.Core.Service.SM3Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.regex.Pattern;

@Service
public class UserService extends BaseUserService<UserDTO> {

    private static final Pattern DNI_PATTERN = Pattern.compile("^\\d{7,11}$");

    @Autowired
    private FrontUrlProperties frontUrlProperties;

    @Autowired
    private CuitValidatorService cuitValidatorService;

    @Autowired
    private MemberUserValidationHelper memberUserValidationHelper;

    @Autowired
    private SM3Service sm3Service;

    @Autowired 
    private CompanyRepository companyRepository;

    /*
     * Hook para permitir la asignación de roles
     * 
     * Se permite la asignación si el rol se encuentra definido en el RoleEnum del
     * proyecto
     * 
     * Si no se desea que esto pase se debe eliminar este método
     */
    @Override
    protected boolean hookCanAssignRole(Role role, User user, String companyUuid) {
        for (RoleEnum roleEnum : RoleEnum.values()) {
            if (roleEnum.name().equals(role.getName())) {
                return true;
            }
        }

        return super.hookCanAssignRole(role, user, companyUuid);
    }

    @Override
    protected User hookSaveMapperDtoToModel(UserDTO dto, User model) {
        validateProfileFields(dto);
        validateEmailUnique(dto);
        memberUserValidationHelper.validateDniUnique(dto.getDni(), dto.getUuid());

        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            String email = dto.getEmail().trim();
            dto.setEmail(email);
            dto.setUsername(email);
        }

        model = super.hookSaveMapperDtoToModel(dto, model);

        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            model.setUsername(dto.getEmail());
            model.setEmail(dto.getEmail());
        }

        return model;
    }

    private void validateProfileFields(UserDTO dto) {
        if (dto.getDni() != null && !dto.getDni().isBlank() && !DNI_PATTERN.matcher(dto.getDni().trim()).matches()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "DNI inválido",
                    "El DNI debe contener entre 7 y 11 dígitos numéricos.");
        }
    }

    private void validateEmailUnique(UserDTO dto) {
        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            return;
        }
        UserRepository userRepo = (UserRepository) this.repository;
        User existingUser = userRepo.findFirstByEmailAndStatusNot(dto.getEmail().trim(), -1);
        if (existingUser != null && (dto.getUuid() == null || !existingUser.getUuid().equals(dto.getUuid()))) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Email ya registrado",
                    "El correo electrónico ya se encuentra en uso.");
        }
    }

    @Override
    protected void hookPublicRegisterPreCreateCompany(UserDTO userDTO, CompanyDTO companyDTO) {
        validateEmailUnique(userDTO);

        // 2. Validar algoritmo del CUIT de la empresa
        if (companyDTO.getCuit() != null && !cuitValidatorService.isValid(companyDTO.getCuit())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "CUIT inválido",
                    "El CUIT ingresado no es válido según su dígito verificador.");
        }
    }

    @Override
    protected MailNotification hookGetMailNotification(String purpose, User user,
            String code, Company company) {
        String frontUrl = frontUrlProperties.getMailUrl();
        if (PURPOSE_FORGOT_PASSWORD.equals(purpose)) {
            if (company == null || company.getLogoUrl() == null || company.getColorPrimario() == null || company.getColorSecundario() == null) {
                company = companyRepository.findFirstByUuid("2"); // Blue Open Data
            }
            return new ForgotPasswordMail(user, code, frontUrl, company);
        }
        if (PURPOSE_REGISTRATION.equals(purpose)) {
            return new RegistrationMail(user, code, frontUrl);
        }
        return super.hookGetMailNotification(purpose, user, code, company);
    }

    public UserDTO uploadPhoto(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Archivo requerido",
                    "Debe enviar un archivo de imagen para la foto de perfil.");
        }

        User user = (User) authService.getActualOrThrow();
        if (user.getImageUrl() != null) {
            deletePhotoFromStorage(user.getImageUrl());
        }

        String imageUrl = sm3Service.upload(file);
        user.setImageUrl(imageUrl);
        user = this.repository.save(user);
        return this.mapper.modelToDto(user);
    }

    public UserDTO deletePhoto() {
        User user = (User) authService.getActualOrThrow();
        deletePhotoFromStorage(user.getImageUrl());
        user.setImageUrl(null);
        user = this.repository.save(user);
        return this.mapper.modelToDto(user);
    }

    private void deletePhotoFromStorage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }
        DeleteArchivoRequest request = new DeleteArchivoRequest();
        request.setUrl(imageUrl);
        sm3Service.delete(request);
    }
}
