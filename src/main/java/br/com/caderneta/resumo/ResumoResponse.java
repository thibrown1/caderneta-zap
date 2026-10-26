package br.com.caderneta.resumo;

import br.com.caderneta.gasto.GastoResponse;
import br.com.caderneta.nucleo.Dinheiro;
import br.com.caderneta.nucleo.RespostasBot;
import br.com.caderneta.nucleo.ResumoCalculado;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Tudo que o painel precisa para desenhar um período.
 *
 * @param fim         último dia do período (inclusive)
 * @param comparacao  frase pronta comparando com o período anterior, ou vazio
 */
public record ResumoResponse(String periodo, int offset, String rotulo, LocalDate inicio, LocalDate fim,
                             boolean emAndamento, double fracaoDecorrida,
                             long total, String totalFormatado, long totalAnterior, String comparacao,
                             long orcamentoPeriodo, long orcamentoMensal,
                             List<CategoriaTotal> porCategoria, List<PessoaTotal> porPessoa,
                             List<DicaResponse> dicas, List<GastoResponse> gastos) {

    public record CategoriaTotal(String codigo, String nome, long total, int percentual) {
    }

    public record PessoaTotal(String autor, long total) {
    }

    public record DicaResponse(String tipo, String rotulo, String texto) {
    }

    public static ResumoResponse de(ResumoService.Calculo c, ZoneId zona) {
        ResumoCalculado r = c.resumo();
        return new ResumoResponse(
                r.periodo().name().toLowerCase(), r.offset(), r.rotulo(),
                r.intervalo().inicio().toLocalDate(), r.intervalo().fim().toLocalDate().minusDays(1),
                r.emAndamento(), r.fracaoDecorrida(),
                r.total(), Dinheiro.formatar(r.total()), r.totalAnterior(), RespostasBot.comparacao(r),
                r.orcamentoPeriodo(), r.orcamentoMensal(),
                r.porCategoria().stream().map(t -> new CategoriaTotal(t.categoria().name(), t.categoria().nome(), t.total(), t.percentual())).toList(),
                r.porPessoa().stream().map(p -> new PessoaTotal(p.autor(), p.total())).toList(),
                c.dicas().stream().map(d -> new DicaResponse(d.tipo().name(), d.tipo().rotulo(), d.texto())).toList(),
                c.gastosDoPeriodo().stream().map(g -> GastoResponse.de(g, zona)).toList());
    }
}
