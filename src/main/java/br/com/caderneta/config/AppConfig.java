package br.com.caderneta.config;

import br.com.caderneta.nucleo.CadastroPessoas;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AppConfig implements WebMvcConfigurer {

    /** Relógio no fuso configurado. Nos testes ele é trocado por um relógio fixo. */
    @Bean
    public Clock clock(CadernetaProperties props) {
        return Clock.system(props.zonaId());
    }

    @Bean
    public CadastroPessoas cadastroPessoas(CadernetaProperties props) {
        return CadastroPessoas.de(props.pessoas());
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/login").setViewName("forward:/login.html");
        // Página pública exigida pela Meta para publicar o app do WhatsApp.
        registry.addViewController("/privacidade").setViewName("forward:/privacidade.html");
    }
}
