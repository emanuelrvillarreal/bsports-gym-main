package com.blue.open.Code.WebSocketConfig;

import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Core.Service.Auth.AuthJwtService;
import com.blue.open.Core.Exceptions.CustomException;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    @Autowired
    private AuthJwtService authJwtService;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        List<String> authorization = accessor.getNativeHeader("Authorization");

        String token = getToken(authorization);

        User user = authJwtService.getUserFromToken(token);
        if (user == null) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "Token inválido o expirado",
                    "WebSocketAuthInterceptor | preSend | User not found or token invalid");
        }

        accessor.setUser(new StompPrincipal(user.getUuid()));
        return message;
    }

    private @NonNull String getToken(List<String> authorization) {
        if (authorization == null || authorization.isEmpty()) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "Falta el token de autorización",
                    "WebSocketAuthInterceptor | preSend | Authorization header empty");
        }

        String bearerToken = authorization.get(0);
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "Formato de token inválido",
                    "WebSocketAuthInterceptor | preSend | Invalid Bearer format");
        }

        return bearerToken.substring(7);
    }
}
