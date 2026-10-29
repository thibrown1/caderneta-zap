package br.com.caderneta.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.caderneta.RelogioDeTeste;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RelogioDeTeste.class)
class ContaEConviteTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    ConviteRepository convites;

    @BeforeEach
    void limpar() {
        convites.deleteAll();
        usuarios.findAll().stream().filter(u -> !u.isAdmin()).forEach(usuarios::delete);
    }

    private String gerarConvite() throws Exception {
        String link = JsonPath.read(mvc.perform(post("/api/convites").with(user("teste").roles("USUARIO", "ADMIN")).with(csrf()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.link");
        assertThat(link).startsWith("https://caderneta.teste/convite?c=");
        return link.substring(link.indexOf("c=") + 2);
    }

    private String aceitar(String codigo, String nome, String login, String senha) {
        return "{\"codigo\":\"" + codigo + "\",\"nome\":\"" + nome + "\",\"login\":\"" + login + "\",\"senha\":\"" + senha + "\"}";
    }

    @Test
    void oDonoDoPainelEhCriadoNaSubidaEEntraComASenhaDoAmbiente() throws Exception {
        assertThat(usuarios.findById("teste")).hasValueSatisfying(u -> assertThat(u.isAdmin()).isTrue());
        mvc.perform(formLogin("/login").user("teste").password("senha-teste")).andExpect(authenticated().withUsername("teste"));
        mvc.perform(formLogin("/login").user("teste").password("errada")).andExpect(unauthenticated());
    }

    @Test
    void conviteCriaUmaContaNovaQueEntraComASenhaEscolhida() throws Exception {
        String codigo = gerarConvite();

        mvc.perform(get("/convite")).andExpect(status().isOk()).andExpect(forwardedUrl("/convite.html"));
        mvc.perform(get("/api/convites/estado").param("c", codigo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valido").value(true))
                .andExpect(jsonPath("$.convidadoPor").value("Teste"));

        mvc.perform(post("/api/convites/aceitar").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(aceitar(codigo, "Larissa", "  Larissa ", "senha-boa-123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.login").value("larissa"))
                .andExpect(jsonPath("$.admin").value(false));

        mvc.perform(formLogin("/login").user("larissa").password("senha-boa-123")).andExpect(authenticated().withUsername("larissa"));
        mvc.perform(get("/api/conta").with(user("larissa"))).andExpect(jsonPath("$.nome").value("Larissa"));

        // o mesmo link não serve duas vezes
        mvc.perform(post("/api/convites/aceitar").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(aceitar(codigo, "Outra", "outra", "senha-boa-123")))
                .andExpect(status().isGone());
        mvc.perform(get("/api/convites/estado").param("c", codigo)).andExpect(status().isGone());
    }

    @Test
    void validaOsDadosDaContaNova() throws Exception {
        String codigo = gerarConvite();
        mvc.perform(post("/api/convites/aceitar").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(aceitar(codigo, "Larissa", "larissa", "curta")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("A senha precisa ter pelo menos 6 caracteres."));
        mvc.perform(post("/api/convites/aceitar").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(aceitar(codigo, "Larissa", "teste", "senha-boa-123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Esse usuário já existe. Escolha outro."));
        mvc.perform(post("/api/convites/aceitar").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(aceitar(codigo, "Larissa", "la ri", "senha-boa-123")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/convites/aceitar").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(aceitar("codigo-inventado", "Larissa", "larissa", "senha-boa-123")))
                .andExpect(status().isGone());
    }

    @Test
    void oDonoCriaAContaDiretoEAPessoaTrocaASenha() throws Exception {
        mvc.perform(post("/api/usuarios").with(user("teste").roles("USUARIO", "ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Larissa\",\"login\":\"larissa\",\"senha\":\"lari12\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.login").value("larissa"));
        mvc.perform(formLogin("/login").user("larissa").password("lari12")).andExpect(authenticated().withUsername("larissa"));

        mvc.perform(put("/api/conta/senha").with(user("larissa")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"atual\":\"errada\",\"nova\":\"outra-senha\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("A senha atual não confere."));
        mvc.perform(put("/api/conta/senha").with(user("larissa")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"atual\":\"lari12\",\"nova\":\"outra-senha\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(formLogin("/login").user("larissa").password("lari12")).andExpect(unauthenticated());
        mvc.perform(formLogin("/login").user("larissa").password("outra-senha")).andExpect(authenticated());

        mvc.perform(post("/api/usuarios").with(user("larissa").roles("USUARIO")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Outra\",\"login\":\"outra\",\"senha\":\"senha-boa\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void soODonoConvidaEVeAsContas() throws Exception {
        mvc.perform(post("/api/convites").with(user("larissa").roles("USUARIO")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(get("/api/usuarios").with(user("larissa").roles("USUARIO"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/convites").with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/usuarios").with(user("teste").roles("USUARIO", "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].login").value("teste"));
    }
}
