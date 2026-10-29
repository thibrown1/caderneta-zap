package br.com.caderneta.usuario;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Quem está usando o painel agora. */
public final class UsuarioAtual {

    private UsuarioAtual() {
    }

    public static String login() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new IllegalStateException("Ninguém entrou no painel.");
        }
        return auth.getName();
    }
}
