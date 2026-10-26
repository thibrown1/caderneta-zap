package br.com.caderneta.gasto;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.caderneta.RelogioDeTeste;
import br.com.caderneta.orcamento.ConfiguracaoRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RelogioDeTeste.class)
class GastoApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    GastoRepository gastos;

    @Autowired
    ConfiguracaoRepository configuracoes;

    @BeforeEach
    void limpar() {
        gastos.deleteAll();
        configuracoes.deleteAll();
    }

    private String criar(String json) throws Exception {
        MvcResult r = mvc.perform(post("/api/gastos").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(r.getResponse().getContentAsString(), "$.id");
    }

    @Test
    void apiExigeLogin() throws Exception {
        mvc.perform(get("/api/gastos")).andExpect(status().isUnauthorized());
    }

    @Test
    void painelRedirecionaParaOLogin() throws Exception {
        mvc.perform(get("/")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser
    void criaListaAtualizaEApaga() throws Exception {
        mvc.perform(post("/api/gastos").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"valorCentavos":4590,"descricao":"gasolina","motivo":"tanque vazio","quando":"2026-10-18T10:00:00-03:00"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.descricao").value("Gasolina"))
                .andExpect(jsonPath("$.categoria").value("TRANSPORTE"))
                .andExpect(jsonPath("$.valor").value("R$ 45,90"));

        String id = JsonPath.read(mvc.perform(get("/api/gastos").param("de", "2026-10-01").param("ate", "2026-10-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andReturn().getResponse().getContentAsString(), "$[0].id");

        mvc.perform(put("/api/gastos/{id}", id).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"valorCentavos":5000,"descricao":"Gasolina","categoria":"TRANSPORTE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valorCentavos").value(5000))
                .andExpect(jsonPath("$.motivo").doesNotExist());

        mvc.perform(delete("/api/gastos/{id}", id).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/gastos/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void validaOsDados() throws Exception {
        mvc.perform(post("/api/gastos").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valorCentavos\":0,\"descricao\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.valorCentavos").exists())
                .andExpect(jsonPath("$.erros.descricao").exists());
    }

    @Test
    @WithMockUser
    void semTokenCsrfEhRecusado() throws Exception {
        mvc.perform(post("/api/gastos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valorCentavos\":100,\"descricao\":\"teste\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser
    void resumoComOrcamento() throws Exception {
        criar("{\"valorCentavos\":20000,\"descricao\":\"Mercado\",\"quando\":\"2026-10-13T10:00:00-03:00\"}");
        criar("{\"valorCentavos\":8000,\"descricao\":\"Pizza\",\"quando\":\"2026-10-17T21:00:00-03:00\"}");
        mvc.perform(put("/api/orcamento").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"valorMensalCentavos\":310000}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/resumo").param("periodo", "semana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rotulo").value("12 a 18 de out"))
                .andExpect(jsonPath("$.total").value(28000))
                .andExpect(jsonPath("$.orcamentoPeriodo").value(70000))
                .andExpect(jsonPath("$.porCategoria[0].codigo").value("MERCADO"))
                .andExpect(jsonPath("$.porCategoria[1].codigo").value("COMIDA_FORA"))
                .andExpect(jsonPath("$.gastos.length()").value(2));

        mvc.perform(get("/api/resumo").param("periodo", "ano")).andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void simuladorConversaComOBot() throws Exception {
        mvc.perform(post("/api/simulador").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"de\":\"5511999998888\",\"texto\":\"32 uber ontem\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resposta").value(org.hamcrest.Matchers.containsString("*R$ 32,00* em Uber")));
    }
}
