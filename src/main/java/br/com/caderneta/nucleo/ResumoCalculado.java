package br.com.caderneta.nucleo;

import java.util.List;

/**
 * Resultado do cálculo de um período.
 *
 * @param totalAnterior     total do período anterior. Se o período atual ainda está em andamento,
 *                          conta só até o mesmo ponto (ex.: segunda a quarta contra segunda a quarta)
 * @param fracaoDecorrida   quanto do período já passou, de 0 a 1 (sempre 1 para períodos fechados)
 * @param orcamentoPeriodo  orçamento do mês, ou a parte proporcional dele na semana; 0 se não houver
 * @param lancamentos       gastos do período, do mais recente para o mais antigo
 */
public record ResumoCalculado(Periodo periodo, int offset, Intervalo intervalo, String rotulo,
                              boolean emAndamento, double fracaoDecorrida,
                              long total, long totalAnterior, long orcamentoPeriodo, long orcamentoMensal,
                              List<TotalCategoria> porCategoria, List<TotalPessoa> porPessoa,
                              List<Lancamento> lancamentos, List<Lancamento> lancamentosAnteriores) {

    public record TotalCategoria(Categoria categoria, long total, int percentual) {
    }

    public record TotalPessoa(String autor, long total) {
    }

    public boolean temOrcamento() {
        return orcamentoPeriodo > 0;
    }

    public int quantidade() {
        return lancamentos.size();
    }
}
