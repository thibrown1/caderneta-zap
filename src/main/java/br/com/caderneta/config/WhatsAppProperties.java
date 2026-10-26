package br.com.caderneta.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Dados do app na Meta (developers.facebook.com).
 *
 * @param token         token de acesso do usuário do sistema (permanente)
 * @param phoneNumberId ID do número de telefone, na tela "Configuração da API" do WhatsApp
 * @param verifyToken   texto que você inventa e repete na tela de webhook da Meta
 * @param appSecret     "Chave secreta do app", usada para conferir a assinatura de cada chamada
 */
@ConfigurationProperties(prefix = "whatsapp")
public record WhatsAppProperties(String token, String phoneNumberId, String verifyToken, String appSecret,
                                 String apiVersion, String baseUrl) {

    public boolean podeEnviar() {
        return StringUtils.hasText(token) && StringUtils.hasText(phoneNumberId);
    }

    public String versao() {
        return StringUtils.hasText(apiVersion) ? apiVersion : "v23.0";
    }

    public String urlBase() {
        return StringUtils.hasText(baseUrl) ? baseUrl : "https://graph.facebook.com";
    }
}
