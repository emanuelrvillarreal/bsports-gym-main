package com.blue.open.Code.Web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AccesoBloqueadoInterceptor accesoBloqueadoInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(this.accesoBloqueadoInterceptor)
                .excludePathPatterns(
                        // Defensa extra: es la unica via por la que un club bloqueado puede destrabarse
                        "/webhookCrm/**",
                        // Un club bloqueado tiene que poder ver y pagar la factura que lo bloquea
                        "/factura/paginate",
                        "/factura/pago",
                        // Login no resuelve company (sin JWT previo) y ya pasaria igual, pero se
                        // excluye explicito. Refresh-token si puede llegar con el JWT/header
                        // todavia vigentes: sin esto, a un usuario bloqueado se le podria vencer
                        // la sesion sin forma de renovarla para llegar a los endpoints de arriba
                        "/user/login",
                        "/user/refresh-token");
    }
}
