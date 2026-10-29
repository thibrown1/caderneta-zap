package br.com.caderneta.whatsapp;

import br.com.caderneta.config.CadernetaProperties;
import br.com.caderneta.gasto.Gasto;
import br.com.caderneta.gasto.GastoService;
import br.com.caderneta.nucleo.CadastroPessoas;
import br.com.caderneta.nucleo.Comando;
import br.com.caderneta.nucleo.Lancamento;
import br.com.caderneta.nucleo.MensagemParser;
import br.com.caderneta.nucleo.Periodo;
import br.com.caderneta.nucleo.RespostasBot;
import br.com.caderneta.nucleo.Telefones;
import br.com.caderneta.orcamento.OrcamentoService;
import br.com.caderneta.resumo.ResumoService;
import br.com.caderneta.usuario.UsuarioService;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/** Recebe a mensagem do WhatsApp, decide o que fazer e responde. */
@Service
public class BotService {

    private static final Logger log = LoggerFactory.getLogger(BotService.class);

    private final GastoService gastos;
    private final ResumoService resumos;
    private final OrcamentoService orcamento;
    private final WhatsAppClient whatsApp;
    private final CadastroPessoas pessoas;
    private final CadernetaProperties props;
    private final MensagensProcessadas processadas;
    private final UsuarioService usuarios;

    public BotService(GastoService gastos, ResumoService resumos, OrcamentoService orcamento, WhatsAppClient whatsApp,
                      CadastroPessoas pessoas, CadernetaProperties props, MensagensProcessadas processadas,
                      UsuarioService usuarios) {
        this.gastos = gastos;
        this.resumos = resumos;
        this.orcamento = orcamento;
        this.whatsApp = whatsApp;
        this.pessoas = pessoas;
        this.props = props;
        this.processadas = processadas;
        this.usuarios = usuarios;
    }

    /** Roda fora da requisição do webhook, porque a Meta espera resposta 200 rápida. */
    @Async
    public void processar(MensagemRecebida mensagem) {
        if (!processadas.primeiraVez(mensagem.id())) {
            log.info("Mensagem {} repetida pela Meta; ignorando.", mensagem.id());
            return;
        }
        try {
            responder(mensagem).ifPresent(resposta -> {
                whatsApp.marcarComoLida(mensagem.id());
                whatsApp.enviarTexto(mensagem.de(), resposta);
            });
        } catch (Exception e) {
            log.error("Erro ao processar a mensagem {} de {}", mensagem.id(), mensagem.de(), e);
        }
    }

    /** Monta a resposta sem enviar nada. Usado pelos testes e pelo simulador. */
    public Optional<String> responder(MensagemRecebida mensagem) {
        String numero = Telefones.normalizar(mensagem.de());
        if (pessoas.vazio()) {
            log.warn("PESSOAS não configurado. Para liberar esse número, use PESSOAS={}=SeuNome", numero);
            return Optional.of(RespostasBot.numeroNaoCadastrado(numero));
        }
        Optional<String> autor = pessoas.nomeDe(numero);
        if (autor.isEmpty()) {
            log.warn("Mensagem ignorada: o número {} não está em PESSOAS.", numero);
            return Optional.empty();
        }
        if (!"text".equals(mensagem.tipo()) || mensagem.texto() == null || mensagem.texto().isBlank()) {
            return Optional.of(RespostasBot.soTexto());
        }
        // Os gastos vão para a conta da pessoa (nome em PESSOAS = nome ou login da conta); sem conta, para o dono.
        String dono = usuarios.loginPorNome(autor.get());

        ZonedDateTime agora = resumos.agora();
        Comando comando = MensagemParser.interpretar(mensagem.texto(), agora.toLocalDate());
        if (comando instanceof Comando.NovoGasto && gastos.mensagemJaAnotada(mensagem.id())) {
            return Optional.empty();
        }
        String url = props.urlSite();

        String resposta = switch (comando) {
            case Comando.NovoGasto g -> anotar(dono, g, autor.get(), numero, mensagem.id(), agora);
            case Comando.Resumo r -> {
                var calculo = resumos.calcular(dono, r.periodo(), r.offset());
                yield RespostasBot.resumo(calculo.resumo(), calculo.dicas(), url);
            }
            case Comando.Hoje h -> RespostasBot.hoje(lancamentosDeHoje(dono, agora), agora);
            case Comando.Ultimos u -> RespostasBot.ultimos(gastos.ultimos(dono).stream().map(Gasto::paraLancamento).toList(), agora);
            case Comando.ApagarUltimo a -> gastos.apagarUltimoDe(numero)
                    .map(g -> RespostasBot.apagado(g.paraLancamento(), agora))
                    .orElseGet(RespostasBot::nadaParaApagar);
            case Comando.Dicas d -> {
                var mes = resumos.calcular(dono, Periodo.MES, 0);
                yield RespostasBot.dicas(mes.dicas(), mes.resumo());
            }
            case Comando.DefinirOrcamento o -> {
                orcamento.definir(dono, o.valorCentavos());
                yield RespostasBot.orcamentoDefinido(o.valorCentavos(), resumos.calcular(dono, Periodo.MES, 0).resumo());
            }
            case Comando.Site s -> RespostasBot.site(url);
            case Comando.Ajuda a -> RespostasBot.ajuda(url);
            case Comando.Incompleto i -> RespostasBot.incompleto(i.valorCentavos());
            case Comando.NaoEntendi n -> RespostasBot.naoEntendi();
        };
        return Optional.of(resposta);
    }

    private String anotar(String dono, Comando.NovoGasto g, String autor, String numero, String idMensagem, ZonedDateTime agora) {
        Gasto salvo = gastos.criarPeloWhatsApp(dono, new GastoService.DadosGasto(g.valorCentavos(), g.descricao(), g.categoria(),
                g.motivo(), g.quando(agora).toInstant(), autor), numero, idMensagem);
        log.info("{} anotou {} centavos em '{}'", autor, salvo.getValorCentavos(), salvo.getDescricao());
        return RespostasBot.gastoAnotado(salvo.paraLancamento(), agora, resumos.calcular(dono, Periodo.SEMANA, 0).resumo());
    }

    private List<Lancamento> lancamentosDeHoje(String dono, ZonedDateTime agora) {
        var inicio = agora.toLocalDate().atStartOfDay(agora.getZone());
        return gastos.listar(dono, inicio.toInstant(), inicio.plusDays(1).toInstant()).stream().map(Gasto::paraLancamento).toList();
    }
}
