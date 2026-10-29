package br.com.caderneta.config;

import br.com.caderneta.nucleo.CadastroPessoas;
import br.com.caderneta.usuario.UsuarioService;
import java.time.Clock;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
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

    /** Senhas guardadas com bcrypt. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /** Na subida, garante que a conta do dono (PAINEL_USUARIO / PAINEL_SENHA) existe. */
    @Bean
    public ApplicationRunner criarDono(UsuarioService usuarios) {
        return args -> usuarios.garantirDono();
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/login").setViewName("forward:/login.html");
        // Página pública exigida pela Meta para publicar o app do WhatsApp.
        registry.addViewController("/privacidade").setViewName("forward:/privacidade.html");
        registry.addViewController("/convite").setViewName("forward:/convite.html");
    }
}
