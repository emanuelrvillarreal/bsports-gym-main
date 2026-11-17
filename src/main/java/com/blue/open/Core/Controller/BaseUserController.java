package com.blue.open.Core.Controller;

import com.blue.open.Core.Entity.DTO.*;
import com.blue.open.Code.Entity.DTO.CompanyDTO;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Core.Service.BaseUserService;
import com.blue.open.Core.Service.Auth.AuthService;
import com.blue.open.Core.Service.CaptchaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

public abstract class BaseUserController<DTO extends BaseUserDTO, S extends BaseUserService<DTO>>
        extends AbstractController<User, DTO, DTO, S> {

    @Autowired
    protected AuthService authService;

    @Autowired
    protected CaptchaService captchaService;

    @Autowired
    protected S service;

    @GetMapping("login")
    public ResponseEntity<DTO> login() {
        User currentUser = (User) authService.valid();
        DTO userDTO = service.getByUuid(currentUser.getUuid());
        return ResponseEntity.ok(userDTO);
    }

    @PostMapping("create-token")
    public ResponseEntity<UserAccessTokenDTO> createToken(@Valid @RequestBody UserAccessTokenDTO dto) {
        return ResponseEntity.ok(service.createAccessTokenDto(dto.getName()));
    }

    @DeleteMapping("delete-token/{uuid}")
    public ResponseEntity<UserAccessTokenDTO> deleteToken(@PathVariable String uuid) {
        return ResponseEntity.ok(service.deleteAccessTokenDto(uuid));
    }

    @GetMapping("tokens")
    public ResponseEntity<List<UserAccessTokenDTO>> getTokens() {
        return ResponseEntity.ok(service.getMyTokenDtos());
    }

    @PostMapping("login")
    public ResponseEntity<JwtTokenDTO> loginJWT(@RequestBody UserAndPasswordDTO body) {
        if (authService.validProtocolBoolean("jwt")) {
            User currentUser = authService.getUserAndPassword(body);
            return ResponseEntity.ok(authService.generateJwtToken(currentUser));
        }
        throw new CustomException("No está disponible este metodo de login");
    }

    @PostMapping("refresh-token")
    public ResponseEntity<JwtTokenDTO> refreshToken(@RequestBody JwtTokenDTO request) {
        String refreshToken = request.getRefresh();
        if (refreshToken == null || refreshToken.isEmpty()) {
            throw new CustomException("Refresh token is missing");
        }
        return ResponseEntity.ok(authService.refreshJwtToken(refreshToken));
    }

    @PostMapping("public/register")
    public ResponseEntity<DTO> publicRegister(
            @RequestBody PublicRegisterRequest<DTO, CompanyDTO> request,
            @RequestHeader(name = "X-Captcha-Token", required = false) String captchaToken) {
        captchaService.verify(captchaToken);
        return ResponseEntity.ok(service.publicRegister(request));
    }

    @PostMapping("public/link/{userUuid}")
    public ResponseEntity<DTO> publicLink(
            @RequestHeader(value = "X-Company-Uuid", required = false) String companyUuid,
            @PathVariable String userUuid) {
        return ResponseEntity.ok(service.publicLink(companyUuid, userUuid));
    }

    @PostMapping("public/register-client")
    public ResponseEntity<DTO> publicRegisterClient(
            @RequestHeader(value = "X-Company-Uuid", required = false) String companyUuid,
            @RequestBody DTO userDTO,
            @RequestHeader(name = "X-Captcha-Token", required = false) String captchaToken) {
        captchaService.verify(captchaToken);
        return ResponseEntity.ok(service.publicRegisterClient(companyUuid, userDTO));
    }

    @PostMapping("public/verify")
    public ResponseEntity<Void> verifyCode(@RequestBody Map<String, String> request) {
        String uuid = request.get("uuid");
        String code = request.get("code");

        if (uuid == null || code == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Datos incompletos",
                    "UUID and Code are required");
        }

        service.verifyCode(uuid, code);
        return ResponseEntity.ok().build();
    }

    @PostMapping("public/resend-code")
    public ResponseEntity<Void> resendCode(@RequestBody DTO userDTO) {
        if (userDTO.getUuid() == null || userDTO.getEmail() == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Datos incompletos",
                    "UUID and Email are required");
        }

        service.resendCode(userDTO.getUuid(), userDTO.getEmail(), null);
        return ResponseEntity.ok().build();
    }

    @PostMapping("public/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String path = request.get("path");
        service.forgotPassword(email, path);
        return ResponseEntity.ok(Map.of("message", "Mail enviado correctamente"));
    }

    @PostMapping("public/change-password")
    public ResponseEntity<ApiResponseDTO> changePassword(@RequestBody ChangePasswordDTO body) {
        service.changePassword(body);
        return ResponseEntity.ok(new ApiResponseDTO(HttpStatus.OK.value(), "Contraseña cambiada correctamente", null));
    }
}
