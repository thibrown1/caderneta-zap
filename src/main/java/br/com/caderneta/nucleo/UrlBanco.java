package br.com.caderneta.nucleo;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Converte o endereço de banco no formato que Neon, Render e Heroku mostram
 * ({@code postgresql://usuario:senha@host/banco?sslmode=require}) para o formato JDBC que o Spring usa.
 * Assim dá pra colar a "connection string" do Neon direto na variável DATABASE_URL.
 */
public final class UrlBanco {

    public record Conexao(String jdbcUrl, String usuario, String senha) {
    }

    private UrlBanco() {
    }

    /** Vazio quando o texto já está em JDBC (jdbc:...) ou não é um endereço Postgres. */
    public static Optional<Conexao> converter(String url) {
        if (url == null) {
            return Optional.empty();
        }
        String u = url.trim();
        if (!u.startsWith("postgres://") && !u.startsWith("postgresql://")) {
            return Optional.empty();
        }
        URI uri;
        try {
            uri = new URI(u);
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return Optional.empty();
        }
        // O Neon oferece um host "-pooler" (PgBouncer). O Flyway usa travas de sessão que não combinam com ele,
        // então a aplicação fala direto com o banco.
        host = host.replace("-pooler.", ".");

        String usuario = "";
        String senha = "";
        if (uri.getRawUserInfo() != null) {
            String[] partes = uri.getRawUserInfo().split(":", 2);
            usuario = decodificar(partes[0]);
            senha = partes.length > 1 ? decodificar(partes[1]) : "";
        }

        String banco = uri.getRawPath() == null || uri.getRawPath().length() <= 1 ? "postgres" : uri.getRawPath().substring(1);
        List<String> parametros = new ArrayList<>();
        if (uri.getRawQuery() != null) {
            for (String p : uri.getRawQuery().split("&")) {
                // channel_binding é do libpq; o driver JDBC não precisa dele.
                if (!p.isBlank() && !p.startsWith("channel_binding=")) {
                    parametros.add(p);
                }
            }
        }
        StringBuilder jdbc = new StringBuilder("jdbc:postgresql://").append(host);
        if (uri.getPort() > 0) {
            jdbc.append(':').append(uri.getPort());
        }
        jdbc.append('/').append(banco);
        if (!parametros.isEmpty()) {
            jdbc.append('?').append(String.join("&", parametros));
        }
        return Optional.of(new Conexao(jdbc.toString(), usuario, senha));
    }

    private static String decodificar(String texto) {
        return URLDecoder.decode(texto, StandardCharsets.UTF_8);
    }
}
