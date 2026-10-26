package br.com.caderneta.resumo;

import br.com.caderneta.config.CadernetaProperties;
import br.com.caderneta.gasto.Gasto;
import br.com.caderneta.gasto.GastoRepository;
import br.com.caderneta.nucleo.CalculadoraResumo;
import br.com.caderneta.nucleo.Dica;
import br.com.caderneta.nucleo.GeradorDicas;
import br.com.caderneta.nucleo.Intervalo;
import br.com.caderneta.nucleo.Periodo;
import br.com.caderneta.nucleo.Periodos;
import br.com.caderneta.nucleo.ResumoCalculado;
import br.com.caderneta.orcamento.OrcamentoService;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Busca no banco só o necessário (período atual + anterior) e entrega para o núcleo calcular. */
@Service
@Transactional(readOnly = true)
public class ResumoService {

    public record Calculo(ResumoCalculado resumo, List<Dica> dicas, List<Gasto> gastosDoPeriodo, ZonedDateTime agora) {
    }

    private final GastoRepository gastos;
    private final OrcamentoService orcamento;
    private final CadernetaProperties props;
    private final Clock clock;

    public ResumoService(GastoRepository gastos, OrcamentoService orcamento, CadernetaProperties props, Clock clock) {
        this.gastos = gastos;
        this.orcamento = orcamento;
        this.props = props;
        this.clock = clock;
    }

    public ZonedDateTime agora() {
        return ZonedDateTime.now(clock).withZoneSameInstant(props.zonaId());
    }

    public Calculo calcular(Periodo periodo, int offset) {
        ZonedDateTime agora = agora();
        Intervalo atual = Periodos.de(periodo, offset, agora);
        Intervalo anterior = Periodos.de(periodo, offset - 1, agora);
        List<Gasto> encontrados = gastos.findByQuandoGreaterThanEqualAndQuandoLessThanOrderByQuandoDesc(
                anterior.inicio().toInstant(), atual.fim().toInstant());
        ResumoCalculado resumo = CalculadoraResumo.calcular(periodo, offset, agora,
                encontrados.stream().map(Gasto::paraLancamento).toList(), orcamento.mensal());
        List<Gasto> doPeriodo = encontrados.stream().filter(g -> atual.contem(g.getQuando())).toList();
        return new Calculo(resumo, GeradorDicas.gerar(resumo, agora), doPeriodo, agora);
    }
}
