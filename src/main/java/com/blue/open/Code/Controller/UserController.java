package com.blue.open.Code.Controller;

import com.blue.open.Code.Entity.DTO.FinishLoginRequestDTO;
import com.blue.open.Code.Entity.DTO.StartLoginRequestDTO;
import com.blue.open.Code.Entity.DTO.StartLoginResponseDTO;
import com.blue.open.Code.Entity.DTO.UserDTO;
import com.blue.open.Code.Service.LoginSinPassword.LoginSinPasswordService;
import com.blue.open.Code.Service.UserService;
import com.blue.open.Core.Controller.BaseUserController;
import com.blue.open.Core.Entity.DTO.JwtTokenDTO;
import com.blue.open.Core.Exceptions.CustomException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("user")
public class UserController extends BaseUserController<UserDTO, UserService> {

    @Autowired
    private LoginSinPasswordService loginSinPasswordService;

    @PostMapping("public/start-login")
    @Operation(summary = "Iniciar login sin contraseña",
            description = "Si el email pertenece a un usuario activo, le envía un mail con un enlace de acceso de un solo uso.")
    public ResponseEntity<StartLoginResponseDTO> startLogin(@Valid @RequestBody StartLoginRequestDTO request) {
        if (!authService.validProtocolBoolean("jwt")) {
            throw new CustomException("No está disponible este metodo de login");
        }
        return ResponseEntity.ok(loginSinPasswordService.iniciarLogin(request));
    }

    @PostMapping("public/finish-login")
    @Operation(summary = "Finalizar login sin contraseña",
            description = "Canjea el código recibido por mail por los tokens JWT, igual que POST login.")
    public ResponseEntity<JwtTokenDTO> finishLogin(@Valid @RequestBody FinishLoginRequestDTO request) {
        if (!authService.validProtocolBoolean("jwt")) {
            throw new CustomException("No está disponible este metodo de login");
        }
        return ResponseEntity.ok(loginSinPasswordService.finalizarLogin(request));
    }

    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir foto de perfil", description = "Reemplaza la foto de perfil del usuario autenticado.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserDTO> uploadPhoto(@RequestPart("file") MultipartFile file) {
        return new ResponseEntity<>(service.uploadPhoto(file), HttpStatus.OK);
    }

    @DeleteMapping("/photo")
    @Operation(summary = "Eliminar foto de perfil", description = "Elimina la foto de perfil del usuario autenticado.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<UserDTO> deletePhoto() {
        return new ResponseEntity<>(service.deletePhoto(), HttpStatus.OK);
    }
}
