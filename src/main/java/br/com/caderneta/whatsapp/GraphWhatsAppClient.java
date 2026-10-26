package br.com.caderneta.whatsapp;

import br.com.caderneta.config.WhatsAppProperties;
import br.com.caderneta.nucleo.Texto;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Envia mensagens pela WhatsApp Cloud API (Graph API da Meta). */
@Component
public class GraphWhatsAppClient implements WhatsAppClient {

    private static final Logger log = LoggerFactory.getLogger(GraphWhatsAppClient.class);
    private static final int LIMITE_TEXTO = 4096;

    private final RestClient http;
    private final WhatsAppProperties props;

    public GraphWhatsAppClient(RestClient.Builder builder, WhatsAppProperties props) {
        this.props = props;
        this.http = builder.baseUrl(props.urlBase()).build();
    }

    @Override
    public void enviarTexto(String para, String texto) {
        if (!props.podeEnviar()) {
            log.warn("WHATSAPP_TOKEN ou WHATSAPP_PHONE_NUMBER_ID vazios; resposta para {} não enviada:\n{}", para, texto);
            return;
        }
        enviar(Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", para,
                "type", "text",
                "text", Map.of("preview_url", false, "body", Texto.limitar(texto, LIMITE_TEXTO))), "resposta para " + para);
    }

    @Override
    public void marcarComoLida(String idMensagem) {
        if (!props.podeEnviar() || idMensagem == null || idMensagem.startsWith("sim-")) {
            return;
        }
        enviar(Map.of("messaging_product", "whatsapp", "status", "read", "message_id", idMensagem), "confirmação de leitura");
    }

    private void enviar(Map<String, Object> corpo, String descricao) {
        try {
            http.post()
                    .uri("/{versao}/{numero}/messages", props.versao(), props.phoneNumberId())
                    .headers(h -> h.setBearerAuth(props.token()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corpo)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            log.error("A Meta recusou a {}: HTTP {} {}", descricao, e.getStatusCode().value(), e.getResponseBodyAsString());
        } catch (RestClientException e) {
            log.error("Falha ao falar com a Meta ({}): {}", descricao, e.getMessage());
        }
    }
}
