package br.com.caderneta.config;

import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configurações gerais, vindas do application.yml ou de variáveis de ambiente.
 *
 * @param urlSite             endereço público do painel, usado nas respostas do bot
 * @param pessoas             quem pode anotar pelo WhatsApp, no formato {@code 5511999999999=Thiago,5511988888888=Larissa}
 * @param simuladorHabilitado liga o endpoint /api/simulador para testar o bot sem WhatsApp
 */
@ConfigurationProperties(prefix = "caderneta")
public record CadernetaProperties(String zona, String urlSite, String usuario, String senha,
                                  String lembrarChave, String pessoas, boolean simuladorHabilitado) {

    public ZoneId zonaId() {
        return zona == null || zona.isBlank() ? ZoneId.of("America/Sao_Paulo") : ZoneId.of(zona);
    }
}
