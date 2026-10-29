package br.com.caderneta.gasto;

import br.com.caderneta.nucleo.Categoria;
import br.com.caderneta.nucleo.ClassificadorCategoria;
import br.com.caderneta.nucleo.Texto;
import br.com.caderneta.usuario.UsuarioService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gastos de cada conta. Todo método recebe o {@code dono}: uma pessoa nunca vê nem mexe nos gastos da outra. */
@Service
@Transactional
public class GastoService {

    /** Dados de um gasto vindos do site ou do WhatsApp. {@code categoria} e {@code quando} podem ser nulos. */
    public record DadosGasto(long valorCentavos, String descricao, Categoria categoria, String motivo,
                             Instant quando, String autor) {
    }

    private final GastoRepository repositorio;
    private final UsuarioService usuarios;
    private final Clock clock;

    public GastoService(GastoRepository repositorio, UsuarioService usuarios, Clock clock) {
        this.repositorio = repositorio;
        this.usuarios = usuarios;
        this.clock = clock;
    }

    public Gasto criarPeloSite(String dono, DadosGasto dados) {
        return repositorio.save(novo(dono, dados, null, Origem.SITE, null));
    }

    public Gasto criarPeloWhatsApp(String dono, DadosGasto dados, String telefone, String waMessageId) {
        return repositorio.save(novo(dono, dados, telefone, Origem.WHATSAPP, waMessageId));
    }

    public Gasto atualizar(String dono, UUID id, DadosGasto dados) {
        Gasto gasto = buscar(dono, id);
        String descricao = Texto.capitalizar(dados.descricao().trim());
        gasto.atualizar(dados.valorCentavos(), descricao, categoriaOuInferida(dono, dados.categoria(), descricao),
                vazioParaNulo(dados.motivo()), dados.quando() != null ? dados.quando() : gasto.getQuando(),
                vazioParaNulo(dados.autor()) != null ? dados.autor().trim() : gasto.getAutor(), Instant.now(clock));
        return gasto;
    }

    public void apagar(String dono, UUID id) {
        repositorio.delete(buscar(dono, id));
    }

    /** Apaga o último gasto que essa pessoa anotou pelo WhatsApp (comando "apagar"). */
    public Optional<Gasto> apagarUltimoDe(String telefone) {
        Optional<Gasto> ultimo = repositorio.findFirstByTelefoneOrderByCriadoEmDesc(telefone);
        ultimo.ifPresent(repositorio::delete);
        return ultimo;
    }

    /** Gasto de outra conta responde "não encontrado", igual a um id que não existe. */
    @Transactional(readOnly = true)
    public Gasto buscar(String dono, UUID id) {
        return repositorio.findByIdAndDono(id, dono).orElseThrow(() -> new GastoNaoEncontradoException(id));
    }

    @Transactional(readOnly = true)
    public List<Gasto> listar(String dono, Instant inicio, Instant fim) {
        return repositorio.findByDonoAndQuandoGreaterThanEqualAndQuandoLessThanOrderByQuandoDesc(dono, inicio, fim);
    }

    @Transactional(readOnly = true)
    public List<Gasto> ultimos(String dono) {
        return repositorio.findTop8ByDonoOrderByQuandoDesc(dono);
    }

    @Transactional(readOnly = true)
    public boolean mensagemJaAnotada(String waMessageId) {
        return waMessageId != null && repositorio.existsByWaMessageId(waMessageId);
    }

    /**
     * Categoria escolhida; se não houver, a mesma que a pessoa usou da última vez para essa descrição;
     * se nunca apareceu, adivinha pelas palavras; no fim, "Outros".
     */
    @Transactional(readOnly = true)
    public Categoria categoriaOuInferida(String dono, Categoria escolhida, String descricao) {
        if (escolhida != null) {
            return escolhida;
        }
        return repositorio.findFirstByDonoAndDescricaoIgnoreCaseAndCategoriaNotOrderByQuandoDesc(dono, descricao.trim(), Categoria.OUTROS)
                .map(Gasto::getCategoria)
                .or(() -> ClassificadorCategoria.inferir(descricao))
                .orElse(Categoria.OUTROS);
    }

    private Gasto novo(String dono, DadosGasto dados, String telefone, Origem origem, String waMessageId) {
        Instant agora = Instant.now(clock);
        String descricao = Texto.capitalizar(dados.descricao().trim());
        // Sem "quem gastou", fica o nome da própria conta.
        String autor = vazioParaNulo(dados.autor()) != null ? dados.autor().trim() : usuarios.nomeDe(dono);
        return new Gasto(dono, dados.valorCentavos(), descricao, categoriaOuInferida(dono, dados.categoria(), descricao),
                vazioParaNulo(dados.motivo()), dados.quando() != null ? dados.quando() : agora,
                autor, telefone, origem, waMessageId, agora);
    }

    private static String vazioParaNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
