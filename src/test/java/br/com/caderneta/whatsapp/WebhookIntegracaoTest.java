package br.com.caderneta.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.caderneta.RelogioDeTeste;
import br.com.caderneta.gasto.Gasto;
import br.com.caderneta.gasto.GastoRepository;
import br.com.caderneta.nucleo.AssinaturaWebhook;
import br.com.caderneta.nucleo.Categoria;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RelogioDeTeste.class)
class WebhookIntegracaoTest {

    private static final String SEGREDO = "segredo-teste";

    @Autowired
    MockMvc mvc;

    @Autowired
    GastoRepository gastos;

    @MockitoBean
    WhatsAppClient whatsApp;

    @BeforeEach
    void limpar() {
        gastos.deleteAll();
    }

    static String payload(String idMensagem, String texto, String de) {
        return """
                {"object":"whatsapp_business_account","entry":[{"id":"1","changes":[{"field":"messages","value":{
                  "messaging_product":"whatsapp","metadata":{"phone_number_id":"123456"},
                  "contacts":[{"profile":{"name":"Thiago"},"wa_id":"%s"}],
                  "messages":[{"from":"%s","id":"%s","timestamp":"1792335600","type":"text","text":{"body":"%s"}}]
                }}]}]}
                """.formatted(de, de, idMensagem, texto);
    }

    private void enviar(String corpo) throws Exception {
        mvc.perform(post("/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Hub-Signature-256", AssinaturaWebhook.assinar(corpo.getBytes(StandardCharsets.UTF_8), SEGREDO))
                        .content(corpo))
                .andExpect(status().isOk());
    }

    @Test
    void verificacaoDaMeta() throws Exception {
        mvc.perform(get("/webhook").param("hub.mode", "subscribe").param("hub.verify_token", "token-teste").param("hub.challenge", "987"))
                .andExpect(status().isOk())
                .andExpect(content().string("987"));
        mvc.perform(get("/webhook").param("hub.mode", "subscribe").param("hub.verify_token", "errado").param("hub.challenge", "987"))
                .andExpect(status().isForbidden());
    }

    @Test
    void recusaChamadaSemAssinatura() throws Exception {
        mvc.perform(post("/webhook").contentType(MediaType.APPLICATION_JSON).content(payload("wamid.0", "45 lanche", "5511999998888")))
                .andExpect(status().isUnauthorized());
        assertThat(gastos.count()).isZero();
    }

    @Test
    void anotaOGastoERespondeNoWhatsApp() throws Exception {
        // a Meta às vezes manda o celular sem o nono dígito
        enviar(payload("wamid.1", "45,90 ifood porque não deu tempo", "551199998888"));

        ArgumentCaptor<String> resposta = ArgumentCaptor.forClass(String.class);
        verify(whatsApp, timeout(3000)).enviarTexto(eq("551199998888"), resposta.capture());
        assertThat(resposta.getValue()).contains("*R$ 45,90* em Ifood").contains("Comida fora").contains("Motivo: não deu tempo");

        Gasto g = gastos.findAll().get(0);
        assertThat(g.getAutor()).isEqualTo("Thiago");
        assertThat(g.getCategoria()).isEqualTo(Categoria.COMIDA_FORA);
        assertThat(g.getTelefone()).isEqualTo("5511999998888");
        assertThat(g.getWaMessageId()).isEqualTo("wamid.1");
    }

    @Test
    void mensagemRepetidaPelaMetaNaoDuplica() throws Exception {
        String corpo = payload("wamid.2", "120 mercado", "5511999998888");
        enviar(corpo);
        verify(whatsApp, timeout(3000)).enviarTexto(anyString(), anyString());
        enviar(corpo);
        verify(whatsApp, after(700).times(1)).enviarTexto(anyString(), anyString());
        assertThat(gastos.count()).isEqualTo(1);
    }

    @Test
    void numeroForaDaListaEhIgnorado() throws Exception {
        enviar(payload("wamid.3", "50 lanche", "5511900001111"));
        verify(whatsApp, after(700).never()).enviarTexto(any(), any());
        assertThat(gastos.count()).isZero();
    }

    @Test
    void respondeOResumoDaSemana() throws Exception {
        enviar(payload("wamid.4", "200 mercado", "5511999998888"));
        verify(whatsApp, timeout(3000)).enviarTexto(anyString(), anyString());
        enviar(payload("wamid.5", "semana", "5511988887777"));
        ArgumentCaptor<String> resposta = ArgumentCaptor.forClass(String.class);
        verify(whatsApp, timeout(3000)).enviarTexto(eq("5511988887777"), resposta.capture());
        assertThat(resposta.getValue()).startsWith("📊 *Semana · 12 a 18 de out*").contains("Total: *R$ 200,00*");
    }
}
