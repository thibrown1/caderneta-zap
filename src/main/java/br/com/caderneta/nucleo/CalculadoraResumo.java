package br.com.caderneta.nucleo;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Soma os gastos de um período e compara com o anterior e com o orçamento. */
public final class CalculadoraResumo {

    private CalculadoraResumo() {
    }

    public static ResumoCalculado calcular(Periodo periodo, int offset, ZonedDateTime agora,
                                           Collection<Lancamento> todos, long orcamentoMensal) {
        Intervalo atual = Periodos.de(periodo, offset, agora);
        Intervalo anterior = Periodos.de(periodo, offset - 1, agora);
        boolean emAndamento = offset == 0;

        double fracao = 1.0;
        Instant corteAnterior = anterior.fim().toInstant();
        if (emAndamento) {
            Duration passou = Duration.between(atual.inicio(), agora);
            fracao = Math.min(1.0, Math.max(0.0, (double) passou.toMillis() / atual.duracao().toMillis()));
            Instant mesmoPonto = anterior.inicio().toInstant().plus(passou);
            if (mesmoPonto.isBefore(corteAnterior)) {
                corteAnterior = mesmoPonto;
            }
        }

        Comparator<Lancamento> maisRecentePrimeiro = Comparator.comparing(Lancamento::quando).reversed();
        List<Lancamento> doPeriodo = todos.stream().filter(l -> atual.contem(l.quando())).sorted(maisRecentePrimeiro).toList();
        Instant inicioAnterior = anterior.inicio().toInstant();
        Instant fimAnterior = corteAnterior;
        List<Lancamento> doAnterior = todos.stream()
                .filter(l -> !l.quando().isBefore(inicioAnterior) && l.quando().isBefore(fimAnterior))
                .sorted(maisRecentePrimeiro).toList();

        long total = soma(doPeriodo);
        long totalAnterior = soma(doAnterior);

        long orcamentoPeriodo = 0;
        if (orcamentoMensal > 0) {
            int diasNoMes = atual.inicio().toLocalDate().lengthOfMonth();
            orcamentoPeriodo = switch (periodo) {
                case MES -> orcamentoMensal;
                case SEMANA -> Math.round(orcamentoMensal * 7.0 / diasNoMes);
                case DIA -> Math.round((double) orcamentoMensal / diasNoMes);
            };
        }

        return new ResumoCalculado(periodo, offset, atual, Periodos.rotulo(periodo, atual), emAndamento, fracao,
                total, totalAnterior, orcamentoPeriodo, Math.max(0, orcamentoMensal),
                porCategoria(doPeriodo, total), porPessoa(doPeriodo), doPeriodo, doAnterior);
    }

    public static long soma(Collection<Lancamento> lancamentos) {
        return lancamentos.stream().mapToLong(Lancamento::valorCentavos).sum();
    }

    public static Map<Categoria, Long> somaPorCategoria(Collection<Lancamento> lancamentos) {
        Map<Categoria, Long> m = new EnumMap<>(Categoria.class);
        for (Lancamento l : lancamentos) {
            m.merge(l.categoria(), l.valorCentavos(), Long::sum);
        }
        return m;
    }

    private static List<ResumoCalculado.TotalCategoria> porCategoria(List<Lancamento> lancamentos, long total) {
        return somaPorCategoria(lancamentos).entrySet().stream()
                .sorted(Map.Entry.<Categoria, Long>comparingByValue().reversed())
                .map(e -> new ResumoCalculado.TotalCategoria(e.getKey(), e.getValue(),
                        total == 0 ? 0 : (int) Math.round(e.getValue() * 100.0 / total)))
                .toList();
    }

    private static List<ResumoCalculado.TotalPessoa> porPessoa(List<Lancamento> lancamentos) {
        Map<String, Long> m = new LinkedHashMap<>();
        for (Lancamento l : lancamentos) {
            String autor = l.autor() == null || l.autor().isBlank() ? "Sem nome" : l.autor();
            m.merge(autor, l.valorCentavos(), Long::sum);
        }
        return m.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> new ResumoCalculado.TotalPessoa(e.getKey(), e.getValue()))
                .toList();
    }
}
