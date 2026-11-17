package com.blue.open.Code.Service.LoginSinPassword;

import com.blue.open.Code.Entity.DTO.FinishLoginRequestDTO;
import com.blue.open.Code.Entity.DTO.StartLoginRequestDTO;
import com.blue.open.Code.Entity.DTO.StartLoginResponseDTO;
import com.blue.open.Code.Entity.Db.CodigoLogin;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Event.LoginSinPasswordSolicitadoEvent;
import com.blue.open.Code.Repository.CodigoLoginRepository;
import com.blue.open.Code.Service.HashService;
import com.blue.open.Core.Entity.DTO.JwtTokenDTO;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Repository.UserRepository;
import com.blue.open.Core.Service.Auth.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;

/**
 * Login sin contraseña: start-login genera un código de un solo uso y dispara el mail con el
 * enlace; finish-login canjea ese código por los tokens JWT.
 */
@Service
@Slf4j
public class LoginSinPasswordService {

    private static final int STATUS_ACTIVO = 0;
    private static final int LONGITUD_CODIGO = 32;
    private static final String CARACTERES_CODIGO =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final String MENSAJE_INICIO = "Mail enviado correctamente";

    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CodigoLoginRepository codigoLoginRepository;

    @Autowired
    private HashService hashService;

    @Autowired
    private AuthService authService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Transactional
    public StartLoginResponseDTO iniciarLogin(StartLoginRequestDTO request) {
        StartLoginResponseDTO respuesta = new StartLoginResponseDTO(MENSAJE_INICIO);

        // Si el email no existe se responde igual, para no revelar qué emails están registrados
        User user = userRepository.findFirstByEmailAndStatus(request.getEmail().trim(), STATUS_ACTIVO);
        if (user == null) {
            log.info("LoginSinPasswordService | iniciarLogin | usuario no encontrado: {}", request.getEmail());
            return respuesta;
        }

        Instant ahora = Instant.now();
        invalidarCodigoVigente(user, ahora);

        String codigoPlano = generarCodigoPlano();
        String codigoHash = hashService.hashear(codigoPlano);
        CodigoLogin codigoLogin = CodigoLogin.crearParaUsuario(user, codigoHash, ahora);
        codigoLoginRepository.save(codigoLogin);

        eventPublisher.publishEvent(new LoginSinPasswordSolicitadoEvent(
                user.getEmail(),
                user.getUsername(),
                codigoPlano,
                request.getPath()));
        return respuesta;
    }

    @Transactional
    public JwtTokenDTO finalizarLogin(FinishLoginRequestDTO request) {
        String codigoHash = hashService.hashear(request.getCode().trim());
        CodigoLogin codigoLogin = codigoLoginRepository.findFirstByCodigoHashAndStatusAndUserStatus(
                codigoHash, STATUS_ACTIVO, STATUS_ACTIVO);
        if (codigoLogin == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Código inválido o vencido",
                    "LoginSinPasswordService | finalizarLogin | código no encontrado");
        }

        codigoLogin.marcarComoUsado(Instant.now());
        codigoLoginRepository.save(codigoLogin);

        return authService.generateJwtToken(codigoLogin.getUser());
    }

    private void invalidarCodigoVigente(User user, Instant ahora) {
        CodigoLogin codigoVigente = codigoLoginRepository.findFirstByUserAndStatusOrderByDateCreatedDesc(
                user, STATUS_ACTIVO);
        if (codigoVigente == null) {
            return;
        }

        codigoVigente.validarFueraDeCooldown(ahora);
        codigoVigente.invalidar(ahora);
        codigoLoginRepository.save(codigoVigente);
    }

    private String generarCodigoPlano() {
        StringBuilder codigo = new StringBuilder(LONGITUD_CODIGO);
        for (int i = 0; i < LONGITUD_CODIGO; i++) {
            int posicion = secureRandom.nextInt(CARACTERES_CODIGO.length());
            codigo.append(CARACTERES_CODIGO.charAt(posicion));
        }
        return codigo.toString();
    }
}
