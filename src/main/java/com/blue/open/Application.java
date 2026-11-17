package com.blue.open;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.TimeZone;

@SpringBootApplication
@EnableAspectJAutoProxy
@EnableScheduling
@EnableJpaRepositories(basePackages = {
        "com.blue.open.Code.Repository",
        "com.blue.open.Core.Repository",
        "com.blue.open.Crm.Repository",
})
@EntityScan(basePackages = {
        "com.blue.open.Code.Entity.Db",
        "com.blue.open.Core.Entity.Db",
        "com.blue.open.Crm.Entity.Db",
})
public class Application {
    public static void main(String[] args) {
        // La zona horaria debe fijarse ANTES de levantar Spring: el driver JDBC toma la zona del JVM al abrir las
        // conexiones. Si se fija después, en un contenedor en UTC las fechas (DATE) se leen un día antes.
        TimeZone.setDefault(TimeZone.getTimeZone("America/Argentina/Buenos_Aires"));
        SpringApplication.run(Application.class, args);
        System.out.println(TimeZone.getDefault());

        // Imprime una contrasena de test
        System.out.println("clave de ejemplo '123': " + new BCryptPasswordEncoder().encode("123"));

        System.out.println("Java Home: " + System.getProperty("java.home"));
        System.out.println(
                "Importar certificado. Ejecutar en powershell: & \""
                        + System.getProperty("java.home")
                        + "\\bin\\keytool.exe\" -importcert -trustcacerts -cacerts -storepass changeit -noprompt -alias sso-malvinas -file \""
                        + System.getProperty("user.dir")
                        + "\\sso-malvinas.crt\""
        );

        System.out.println(">>> INICIADO CORRECTAMENTE <<< " + new java.util.Date());
    }
}
