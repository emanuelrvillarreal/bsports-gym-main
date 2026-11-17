package com.blue.open.Core.Service.Auth;

import com.blue.open.Core.Entity.DTO.JwtTokenDTO;
import com.blue.open.Core.Entity.DTO.UserAndPasswordDTO;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Core.Repository.UserRepository;
import com.blue.open.Core.Entity.Db.JwtRefreshToken;
import com.blue.open.Core.Repository.JwtRefreshTokenRepository;
import com.blue.open.Core.Exceptions.CustomException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class AuthJwtService {

    @Value("${jwt.secret:defaultSecretKeyThatIsVeryLongAndSecureEnoughForHS256AlgorithmToWorkProperly1234567890}")
    private String jwtSecret;

    private final long tokenExpirationMs = 300000; // 5 minutes
    private final long refreshTokenExpirationMs = 172800000; // 2 days

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtRefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Transactional
    public JwtTokenDTO generateToken(User user) {
        // Invalidate all previous refresh tokens for this user
        refreshTokenRepository.deleteByUser(user);

        // Create and persist the new refresh token
        JwtRefreshToken refreshToken = new JwtRefreshToken();
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenExpirationMs));
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken = refreshTokenRepository.save(refreshToken);

        // Create the access token
        Date now = new Date();
        Date tokenExpiryDate = new Date(now.getTime() + tokenExpirationMs);
        String accessToken = Jwts.builder()
                .setSubject(user.getUuid())
                .claim("type", "access")
                .setIssuedAt(now)
                .setExpiration(tokenExpiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();

        JwtTokenDTO jwtTokenDTO = new JwtTokenDTO();
        jwtTokenDTO.setToken(accessToken);
        jwtTokenDTO.setRefresh(refreshToken.getToken());

        return jwtTokenDTO;
    }

    @Transactional
    public JwtTokenDTO refreshToken(String requestRefreshToken) {
        JwtRefreshToken refreshToken = refreshTokenRepository.findByToken(requestRefreshToken)
                .orElseThrow(
                        () -> new CustomException(HttpStatus.UNAUTHORIZED, "Refresh token inválido o no encontrado",
                                "AuthJwtService | refreshToken | Token not found: " + requestRefreshToken));

        // Verify expiration
        if (refreshToken.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(refreshToken);
            throw new CustomException(HttpStatus.UNAUTHORIZED,
                    "El refresh token ha expirado. Por favor inicie sesión nuevamente",
                    "AuthJwtService | refreshToken | verifyExpiration | Token expired");
        }

        // Invalidate the used token
        User user = refreshToken.getUser();
        refreshTokenRepository.delete(refreshToken);

        // Issue a new refresh token
        JwtRefreshToken newRefreshToken = new JwtRefreshToken();
        newRefreshToken.setUser(user);
        newRefreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenExpirationMs));
        newRefreshToken.setToken(UUID.randomUUID().toString());
        newRefreshToken = refreshTokenRepository.save(newRefreshToken);

        // Issue a new access token
        Date now = new Date();
        Date tokenExpiryDate = new Date(now.getTime() + tokenExpirationMs);
        String newAccessToken = Jwts.builder()
                .setSubject(user.getUuid())
                .claim("type", "access")
                .setIssuedAt(now)
                .setExpiration(tokenExpiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();

        JwtTokenDTO jwtTokenDTO = new JwtTokenDTO();
        jwtTokenDTO.setToken(newAccessToken);
        jwtTokenDTO.setRefresh(newRefreshToken.getToken());
        return jwtTokenDTO;
    }

    public User getUser() {
        String token = getTokenFromRequest();
        if (token == null) {
            return null;
        }
        if (!isJwtFormat(token)) {
            return null;
        }
        try {
            return resolveUserFromAccessToken(token, "AuthJwtService | getUser");
        } catch (CustomException e) {
            throw e;
        } catch (ExpiredJwtException e) {
            throw new CustomException(HttpStatus.UNAUTHORIZED,
                    "El token de acceso ha expirado. Por favor inicie sesión nuevamente",
                    "AuthJwtService | getUser | Token expired");
        } catch (Exception e) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "Token de acceso inválido",
                    "AuthJwtService | getUser | error: " + e.getMessage());
        }
    }

    public User getUserFromToken(String token) {
        if (token == null || token.isEmpty()) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "Token de acceso requerido",
                    "AuthJwtService | getUserFromToken | Token vacío o nulo");
        }
        try {
            return resolveUserFromAccessToken(token, "AuthJwtService | getUserFromToken");
        } catch (CustomException e) {
            throw e;
        } catch (ExpiredJwtException e) {
            throw new CustomException(HttpStatus.UNAUTHORIZED,
                    "El token de acceso ha expirado. Por favor inicie sesión nuevamente",
                    "AuthJwtService | getUserFromToken | Token expired");
        } catch (Exception e) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "Token de acceso inválido",
                    "AuthJwtService | getUserFromToken | error: " + e.getMessage());
        }
    }

    private User resolveUserFromAccessToken(String token, String context) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        String type = claims.get("type", String.class);
        if (!"access".equals(type)) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "Token de acceso inválido",
                    context + " | Tipo de token no permitido: " + type);
        }

        String uuid = claims.getSubject();
        if (uuid == null || uuid.isEmpty()) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "Token de acceso inválido",
                    context + " | Subject ausente en el token");
        }

        User user = userRepository.findFirstByUuidWithStatusIsActive(uuid);
        if (user == null) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado o inactivo",
                    context + " | Usuario no encontrado para uuid: " + uuid);
        }
        return user;
    }

    private boolean isJwtFormat(String token) {
        return token.split("\\.", -1).length == 3;
    }

    private String getTokenFromRequest() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes();
        if (requestAttributes != null) {
            HttpServletRequest request = requestAttributes.getRequest();
            String bearerToken = request.getHeader("Authorization");

            if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
                return bearerToken.substring(7);
            }
        }
        return null;
    }

    public User getUserAndPassword(UserAndPasswordDTO body) {
        User user = userRepository.findFirstByUsernameAndStatus(body.getUsername(), 0);
        if (user == null) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos",
                    "AuthJwtService | getUserAndPassword | User not found: " + body.getUsername());
        }

        if (!passwordEncoder.matches(body.getPassword(), user.getPassword())) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos",
                    "AuthJwtService | getUserAndPassword | Password mismatch for user: " + body.getUsername());
        }

        return user;
    }
}
