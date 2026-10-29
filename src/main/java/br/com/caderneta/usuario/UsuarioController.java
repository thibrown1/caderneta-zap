package br.com.caderneta.usuario;

import br.com.caderneta.config.CadernetaProperties;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api")
public class UsuarioController {

    public record Conta(String login, String nome, boolean admin) {
        static Conta de(Usuario u) {
            return new Conta(u.getLogin(), u.getNome(), u.isAdmin());
        }
    }

    public record ConviteResponse(String link) {
    }

    public record EstadoConvite(boolean valido, String convidadoPor) {
    }

    public record AceitarConvite(String codigo, String nome, String login, String senha) {
    }

    public record NovaConta(String nome, String login, String senha) {
    }

    public record TrocaDeSenha(String atual, String nova) {
    }

    private final UsuarioService servico;
    private final CadernetaProperties props;

    public UsuarioController(UsuarioService servico, CadernetaProperties props) {
        this.servico = servico;
        this.props = props;
    }

    /** Quem está logado. */
    @GetMapping("/conta")
    public Conta conta() {
        String login = UsuarioAtual.login();
        return servico.buscar(login).map(Conta::de)
                .orElseGet(() -> new Conta(login, servico.nomeDe(login), false));
    }

    /** Todas as contas. Só o dono vê. */
    @GetMapping("/usuarios")
    public List<Conta> usuarios() {
        return servico.listar().stream().map(Conta::de).toList();
    }

    /** O dono cria a conta de outra pessoa direto, escolhendo usuário e senha. */
    @PostMapping("/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    public Conta criar(@RequestBody NovaConta pedido) {
        return Conta.de(servico.criarConta(pedido.nome(), pedido.login(), pedido.senha()));
    }

    /** Quem está logado troca a própria senha. */
    @PutMapping("/conta/senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void trocarSenha(@RequestBody TrocaDeSenha pedido) {
        servico.trocarSenha(UsuarioAtual.login(), pedido.atual(), pedido.nova());
    }

    /** Gera um link de convite. Só o dono pode. */
    @PostMapping("/convites")
    @ResponseStatus(HttpStatus.CREATED)
    public ConviteResponse convidar() {
        UsuarioService.NovoConvite novo = servico.criarConvite(UsuarioAtual.login());
        String base = StringUtils.hasText(props.urlSite()) && !props.urlSite().contains("localhost")
                ? props.urlSite().replaceAll("/+$", "")
                : ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        return new ConviteResponse(base + "/convite?c=" + novo.codigo());
    }

    /** A página do convite pergunta se o link ainda vale antes de mostrar o formulário. Aberto, sem login. */
    @GetMapping("/convites/estado")
    public EstadoConvite estado(@RequestParam("c") String codigo) {
        Convite convite = servico.conviteValido(codigo);
        return new EstadoConvite(true, servico.nomeDe(convite.getCriadoPor()));
    }

    /** Cria a conta da pessoa convidada. Aberto, sem login. */
    @PostMapping("/convites/aceitar")
    @ResponseStatus(HttpStatus.CREATED)
    public Conta aceitar(@RequestBody AceitarConvite pedido) {
        return Conta.de(servico.aceitarConvite(pedido.codigo(), pedido.nome(), pedido.login(), pedido.senha()));
    }
}
