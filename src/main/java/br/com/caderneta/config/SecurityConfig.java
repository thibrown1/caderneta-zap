package br.com.caderneta.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Painel e API protegidos por login. O webhook fica aberto porque quem chama é a Meta,
 * e cada chamada é conferida pela assinatura HMAC no {@code WebhookController}.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filtros(HttpSecurity http, CadernetaProperties props, UserDetailsService usuarios) throws Exception {
        RequestMatcher api = request -> request.getRequestURI().startsWith(request.getContextPath() + "/api/");
        // Sem login: a API responde 401 (o painel trata e manda para o login); páginas redirecionam para /login.
        AuthenticationEntryPoint naoAutorizado = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
        AuthenticationEntryPoint paginaDeLogin = new LoginUrlAuthenticationEntryPoint("/login");

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/webhook", "/login", "/login.html", "/privacidade", "/privacidade.html",
                                "/favicon.svg", "/actuator/health", "/error").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?erro")
                        .permitAll())
                .rememberMe(lembrar -> lembrar
                        .key(chaveLembrar(props))
                        .rememberMeParameter("lembrar")
                        .tokenValiditySeconds((int) Duration.ofDays(60).toSeconds())
                        .userDetailsService(usuarios))
                .logout(sair -> sair.logoutUrl("/logout").logoutSuccessUrl("/login?saiu"))
                // O painel lê o token do cookie XSRF-TOKEN e devolve no cabeçalho X-XSRF-TOKEN.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers("/webhook"))
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .exceptionHandling(erros -> erros.authenticationEntryPoint((request, response, excecao) ->
                        (api.matches(request) ? naoAutorizado : paginaDeLogin).commence(request, response, excecao)))
                .headers(cabecalhos -> cabecalhos.contentSecurityPolicy(csp -> csp.policyDirectives(
                        "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
                                + "font-src https://fonts.gstatic.com; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'")));
        return http.build();
    }

    @Bean
    UserDetailsService usuarios(CadernetaProperties props) {
        if (!StringUtils.hasText(props.senha())) {
            throw new IllegalStateException("Defina a senha do painel na variável PAINEL_SENHA.");
        }
        // A senha vem de variável de ambiente e fica só na memória. Guardada sem hash aleatório,
        // o cookie "continuar conectado" continua valendo depois que o servidor reinicia
        // (no Render grátis ele hiberna e reinicia toda hora).
        return new InMemoryUserDetailsManager(User.withUsername(props.usuario())
                .password("{noop}" + props.senha())
                .roles("DONO")
                .build());
    }

    /**
     * Chave que assina o cookie "continuar conectado". Sem PAINEL_LEMBRAR_CHAVE, deriva uma da senha:
     * trocar a senha desconecta todos os aparelhos, que é o esperado.
     */
    static String chaveLembrar(CadernetaProperties props) {
        String chave = props.lembrarChave();
        if (StringUtils.hasText(chave) && !"troque-esta-chave".equals(chave)) {
            return chave;
        }
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(("caderneta-lembrar:" + props.usuario() + ":" + props.senha()).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /** Força a geração do token CSRF em toda resposta, para o cookie existir antes do primeiro POST. */
    static final class CsrfCookieFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws ServletException, IOException {
            CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (token != null) {
                token.getToken();
            }
            chain.doFilter(request, response);
        }
    }
}
