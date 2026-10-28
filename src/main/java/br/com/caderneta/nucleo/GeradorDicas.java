package br.com.caderneta.nucleo;

import static br.com.caderneta.nucleo.Dinheiro.formatar;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Regras simples que olham o período e apontam onde dá para economizar. */
public final class GeradorDicas {

    public static final int MAXIMO = 3;
    private static final long PEQUENO = 30_00;

    private GeradorDicas() {
    }

    public static List<Dica> gerar(ResumoCalculado r, ZonedDateTime agora) {
        List<Dica> dicas = new ArrayList<>();
        if (r.lancamentos().isEmpty()) {
            return dicas;
        }
        Periodo periodo = r.periodo();
        boolean semana = periodo == Periodo.SEMANA;
        boolean dia = periodo == Periodo.DIA;
        String doPeriodo = switch (periodo) { case DIA -> "do dia"; case SEMANA -> "da semana"; case MES -> "do mês"; };
        String este = switch (periodo) { case DIA -> "hoje"; case SEMANA -> "esta semana"; case MES -> "este mês"; };
        long total = r.total();
        long orcamento = r.orcamentoPeriodo();

        if (orcamento > 0) {
            if (total > orcamento) {
                dicas.add(new Dica(Dica.Tipo.ALERTA, "O orçamento " + doPeriodo + " já estourou em " + formatar(total - orcamento) + "."
                        + (r.emAndamento() ? " Até o fim do período, segure o que não for essencial." : "")));
            } else if (dia && r.emAndamento()) {
                // Num dia só, projetar pelo relógio não faz sentido (um café às 8h não vira 12 cafés). Mostra o que ainda cabe.
                dicas.add(new Dica(Dica.Tipo.BOA, "Hoje ainda cabem " + formatar(orcamento - total) + " no orçamento do dia."));
            } else if (r.emAndamento() && r.fracaoDecorrida() >= 0.1) {
                long projecao = Math.round(total / r.fracaoDecorrida());
                long diasRestantes = Math.max(1, (long) Math.ceil(Duration.between(agora, r.intervalo().fim()).toMinutes() / 1440.0));
                if (projecao > orcamento) {
                    dicas.add(new Dica(Dica.Tipo.ATENCAO, "No ritmo atual, " + este + " fecha em " + formatar(projecao) + ", "
                            + formatar(projecao - orcamento) + " acima do orçamento. Pra ficar dentro, o limite é "
                            + formatar((orcamento - total) / diasRestantes) + " por dia até o fim."));
                } else {
                    dicas.add(new Dica(Dica.Tipo.BOA, "No ritmo atual, " + este + " fecha com " + formatar(orcamento - projecao)
                            + " sobrando do orçamento. Se der, já separa esse valor pra reserva."));
                }
            } else if (!r.emAndamento()) {
                dicas.add(new Dica(Dica.Tipo.BOA, "Fechou dentro do orçamento, com " + formatar(orcamento - total) + " de sobra."));
            }
        }

        Map<Categoria, Long> atual = CalculadoraResumo.somaPorCategoria(r.lancamentos());
        Map<Categoria, Long> anterior = CalculadoraResumo.somaPorCategoria(r.lancamentosAnteriores());
        Categoria maiorAlta = null;
        long maiorDiferenca = 0;
        for (var e : atual.entrySet()) {
            long antes = anterior.getOrDefault(e.getKey(), 0L);
            long diferenca = e.getValue() - antes;
            if (antes > 0 && diferenca >= 50_00 && e.getValue() >= antes * 1.25 && diferenca > maiorDiferenca) {
                maiorAlta = e.getKey();
                maiorDiferenca = diferenca;
            }
        }
        if (maiorAlta != null) {
            long antes = anterior.get(maiorAlta);
            long pct = Math.round(maiorDiferenca * 100.0 / antes);
            String comparacao = switch (periodo) {
                case DIA -> r.emAndamento() ? "em relação a ontem até esta hora" : "em relação ao dia anterior";
                case SEMANA -> r.emAndamento() ? "em relação ao mesmo ponto da semana passada" : "em relação à semana anterior";
                case MES -> r.emAndamento() ? "em relação ao mesmo ponto do mês passado" : "em relação ao mês anterior";
            };
            dicas.add(new Dica(Dica.Tipo.ATENCAO, maiorAlta.nome() + " subiu " + pct + "% " + comparacao + " (+" + formatar(maiorDiferenca) + ")."));
        }

        List<Lancamento> pequenos = r.lancamentos().stream().filter(l -> l.valorCentavos() <= PEQUENO).toList();
        if (pequenos.size() >= (dia ? 3 : semana ? 4 : 8)) {
            dicas.add(new Dica(Dica.Tipo.ATENCAO, pequenos.size() + " gastos pequenos, de até R$ 30, somaram "
                    + formatar(CalculadoraResumo.soma(pequenos))
                    + ". Um por um parece pouco, mas juntos pesam. Um teto semanal pra esses ajuda bastante."));
        }

        // Um dia só não diz quanto aquilo pesa no mês, então essa conta fica para a semana e o mês.
        atual.entrySet().stream()
                .filter(e -> !dia)
                .filter(e -> e.getKey().cortavel())
                .max(Map.Entry.comparingByValue())
                .filter(e -> e.getValue() >= 20_00 && e.getValue() >= total * 0.1)
                .ifPresent(e -> {
                    double mensal = semana ? e.getValue() * 52.0 / 12.0 : e.getValue();
                    long economia = Math.round(mensal * 0.3);
                    long pct = Math.round(e.getValue() * 100.0 / total);
                    dicas.add(new Dica(Dica.Tipo.IDEIA, e.getKey().nome() + " levou " + formatar(e.getValue()) + " (" + pct
                            + "% do período). Cortando 30% disso, sobram " + (r.emAndamento() ? "pelo menos " : "")
                            + formatar(economia) + " por mês, ou " + formatar(economia * 12) + " num ano."));
                });

        long semMotivo = r.lancamentos().stream().filter(l -> !l.temMotivo()).count();
        if (r.quantidade() >= 5 && semMotivo > r.quantidade() * 0.6) {
            dicas.add(new Dica(Dica.Tipo.IDEIA, "Anote o porquê dos gastos. Assim fica fácil separar o que foi necessidade do que foi impulso."));
        }

        return dicas.stream().sorted(Comparator.comparing(Dica::tipo)).limit(MAXIMO).toList();
    }
}
