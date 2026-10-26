package br.com.caderneta.whatsapp;

import br.com.caderneta.config.WhatsAppProperties;
import br.com.caderneta.nucleo.AssinaturaWebhook;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint que a Meta chama. Configure a URL https://SEU_DOMINIO/webhook no painel do app. */
@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final WhatsAppProperties props;
    private final BotService bot;
    private final ObjectMapper json;

    public WebhookController(WhatsAppProperties props, BotService bot, ObjectMapper json) {
        this.props = props;
        this.bot = bot;
        this.json = json;
    }

    /** Verificação feita uma vez quando você salva a URL do webhook na Meta. */
    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verificar(@RequestParam(name = "hub.mode", required = false) String modo,
                                            @RequestParam(name = "hub.verify_token", required = false) String token,
                                            @RequestParam(name = "hub.challenge", required = false) String desafio) {
        boolean confere = "subscribe".equals(modo) && StringUtils.hasText(props.verifyToken())
                && props.verifyToken().equals(token) && desafio != null;
        if (!confere) {
            log.warn("Verificação do webhook recusada: o verify token não confere com WHATSAPP_VERIFY_TOKEN.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        log.info("Webhook verificado pela Meta.");
        return ResponseEntity.ok(desafio);
    }

    @PostMapping
    public ResponseEntity<Void> receber(@RequestBody byte[] corpo,
                                        @RequestHeader(name = "X-Hub-Signature-256", required = false) String assinatura) {
        if (!AssinaturaWebhook.valida(corpo, assinatura, props.appSecret())) {
            log.warn("Chamada ao webhook recusada: assinatura ausente ou inválida (confira WHATSAPP_APP_SECRET).");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        WebhookPayload payload;
        try {
            payload = json.readValue(corpo, WebhookPayload.class);
        } catch (IOException e) {
            log.warn("JSON do webhook ilegível: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
        payload.mensagens().forEach(bot::processar);
        return ResponseEntity.ok().build();
    }
}
