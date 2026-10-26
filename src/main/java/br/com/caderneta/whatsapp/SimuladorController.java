package br.com.caderneta.whatsapp;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Conversa com o bot sem WhatsApp, para testar no Postman antes de configurar a Meta.
 * Só existe com {@code caderneta.simulador-habilitado=true} (perfil "local" ou SIMULADOR=true).
 */
@RestController
@RequestMapping("/api/simulador")
@ConditionalOnProperty(prefix = "caderneta", name = "simulador-habilitado", havingValue = "true")
public class SimuladorController {

    public record Pedido(@NotBlank String de, @NotBlank String texto) {
    }

    public record Resposta(String resposta) {
    }

    private final BotService bot;

    public SimuladorController(BotService bot) {
        this.bot = bot;
    }

    @PostMapping
    public Resposta conversar(@Valid @RequestBody Pedido pedido) {
        var mensagem = new MensagemRecebida("sim-" + UUID.randomUUID(), pedido.de(), null, "text", pedido.texto());
        return new Resposta(bot.responder(mensagem).orElse("(sem resposta: esse número não está em PESSOAS)"));
    }
}
