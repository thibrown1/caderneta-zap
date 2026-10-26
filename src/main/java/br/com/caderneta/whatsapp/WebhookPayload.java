package br.com.caderneta.whatsapp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parte do JSON que a Meta envia ao webhook que interessa aqui. Exemplo resumido:
 * <pre>
 * {"object":"whatsapp_business_account","entry":[{"changes":[{"field":"messages","value":{
 *   "contacts":[{"profile":{"name":"Thiago"},"wa_id":"5511999998888"}],
 *   "messages":[{"from":"5511999998888","id":"wamid.X","timestamp":"1760780000","type":"text","text":{"body":"45 lanche"}}]
 * }}]}]}
 * </pre>
 * Avisos de status (entregue, lido) chegam no mesmo endpoint, sem "messages", e são ignorados.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WebhookPayload(String object, List<Entry> entry) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Entry(String id, List<Change> changes) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Change(String field, Value value) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Value(List<Contact> contacts, List<Message> messages) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Contact(Profile profile, @JsonProperty("wa_id") String waId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Profile(String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(String from, String id, String timestamp, String type, Text text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Text(String body) {
    }

    public List<MensagemRecebida> mensagens() {
        List<MensagemRecebida> resultado = new ArrayList<>();
        if (entry == null) {
            return resultado;
        }
        for (Entry e : entry) {
            if (e == null || e.changes() == null) {
                continue;
            }
            for (Change c : e.changes()) {
                if (c == null || c.value() == null || c.value().messages() == null) {
                    continue;
                }
                Map<String, String> nomes = new HashMap<>();
                if (c.value().contacts() != null) {
                    for (Contact contato : c.value().contacts()) {
                        if (contato != null && contato.waId() != null && contato.profile() != null) {
                            nomes.put(contato.waId(), contato.profile().name());
                        }
                    }
                }
                for (Message m : c.value().messages()) {
                    if (m == null || m.from() == null) {
                        continue;
                    }
                    String texto = m.text() == null ? null : m.text().body();
                    resultado.add(new MensagemRecebida(m.id(), m.from(), nomes.get(m.from()), m.type(), texto));
                }
            }
        }
        return resultado;
    }
}
