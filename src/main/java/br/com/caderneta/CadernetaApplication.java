package br.com.caderneta;

import br.com.caderneta.nucleo.UrlBanco;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@ConfigurationPropertiesScan
public class CadernetaApplication {

    public static void main(String[] args) {
        // Aceita DATABASE_URL no formato postgresql://usuario:senha@host/banco (Neon, Render, Heroku).
        UrlBanco.converter(System.getenv("DATABASE_URL")).ifPresent(banco -> {
            System.setProperty("spring.datasource.url", banco.jdbcUrl());
            System.setProperty("spring.datasource.username", banco.usuario());
            System.setProperty("spring.datasource.password", banco.senha());
        });
        SpringApplication.run(CadernetaApplication.class, args);
    }
}
