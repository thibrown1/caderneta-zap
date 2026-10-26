package br.com.caderneta.gasto;

import br.com.caderneta.nucleo.Categoria;
import br.com.caderneta.nucleo.ClassificadorCategoria;
import br.com.caderneta.nucleo.Texto;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GastoService {

    /** Dados de um gasto vindos do site ou do WhatsApp. {@code categoria} e {@code quando} podem ser nulos. */
    public record DadosGasto(long valorCentavos, String descricao, Categoria categoria, String motivo,
                             Instant quando, String autor) {
    }

    private final GastoRepository repositorio;
    private final Clock clock;

    public GastoService(GastoRepository repositorio, Clock clock) {
        this.repositorio = repositorio;
        this.clock = clock;
    }

    public Gasto criarPeloSite(DadosGasto dados) {
        return repositorio.save(novo(dados, null, Origem.SITE, null));
    }

    public Gasto criarPeloWhatsApp(DadosGasto dados, String telefone, String waMessageId) {
        return repositorio.save(novo(dados, telefone, Origem.WHATSAPP, waMessageId));
    }

    public Gasto atualizar(UUID id, DadosGasto dados) {
        Gasto gasto = buscar(id);
        String descricao = Texto.capitalizar(dados.descricao().trim());
        gasto.atualizar(dados.valorCentavos(), descricao, categoriaOuInferida(dados.categoria(), descricao),
                vazioParaNulo(dados.motivo()), dados.quando() != null ? dados.quando() : gasto.getQuando(),
                vazioParaNulo(dados.autor()) != null ? dados.autor().trim() : gasto.getAutor(), Instant.now(clock));
        return gasto;
    }

    public void apagar(UUID id) {
        repositorio.delete(buscar(id));
    }

    /** Apaga o último gasto que essa pessoa anotou pelo WhatsApp (comando "apagar"). */
    public Optional<Gasto> apagarUltimoDe(String telefone) {
        Optional<Gasto> ultimo = repositorio.findFirstByTelefoneOrderByCriadoEmDesc(telefone);
        ultimo.ifPresent(repositorio::delete);
        return ultimo;
    }

    @Transactional(readOnly = true)
    public Gasto buscar(UUID id) {
        return repositorio.findById(id).orElseThrow(() -> new GastoNaoEncontradoException(id));
    }

    @Transactional(readOnly = true)
    public List<Gasto> listar(Instant inicio, Instant fim) {
        return repositorio.findByQuandoGreaterThanEqualAndQuandoLessThanOrderByQuandoDesc(inicio, fim);
    }

    @Transactional(readOnly = true)
    public List<Gasto> ultimos() {
        return repositorio.findTop8ByOrderByQuandoDesc();
    }

    @Transactional(readOnly = true)
    public boolean mensagemJaAnotada(String waMessageId) {
        return waMessageId != null && repositorio.existsByWaMessageId(waMessageId);
    }

    /**
     * Categoria escolhida; se não houver, a mesma usada da última vez para essa descrição;
     * se nunca apareceu, adivinha pelas palavras; no fim, "Outros".
     */
    @Transactional(readOnly = true)
    public Categoria categoriaOuInferida(Categoria escolhida, String descricao) {
        if (escolhida != null) {
            return escolhida;
        }
        return repositorio.findFirstByDescricaoIgnoreCaseAndCategoriaNotOrderByQuandoDesc(descricao.trim(), Categoria.OUTROS)
                .map(Gasto::getCategoria)
                .or(() -> ClassificadorCategoria.inferir(descricao))
                .orElse(Categoria.OUTROS);
    }

    private Gasto novo(DadosGasto dados, String telefone, Origem origem, String waMessageId) {
        Instant agora = Instant.now(clock);
        String descricao = Texto.capitalizar(dados.descricao().trim());
        return new Gasto(dados.valorCentavos(), descricao, categoriaOuInferida(dados.categoria(), descricao),
                vazioParaNulo(dados.motivo()), dados.quando() != null ? dados.quando() : agora,
                vazioParaNulo(dados.autor()), telefone, origem, waMessageId, agora);
    }

    private static String vazioParaNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
