package br.com.caderneta.nucleo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculadoraResumoTest {

    static final ZoneId SP = ZoneId.of("America/Sao_Paulo");
    /** Domingo, 18/10/2026, meio-dia: a semana atual está 92,86% decorrida. */
    static final ZonedDateTime AGORA = ZonedDateTime.of(2026, 10, 18, 12, 0, 0, 0, SP);

    static Lancamento l(int dia, int hora, long centavos, Categoria cat, String autor) {
        return new Lancamento("id-" + dia + "-" + hora, centavos, cat,
                ZonedDateTime.of(2026, 10, dia, hora, 0, 0, 0, SP).toInstant(), autor, "gasto", "");
    }

    static final List<Lancamento> DADOS = List.of(
            // semana atual (12 a 18/10)
            l(13, 10, 200_00, Categoria.MERCADO, "Thiago"),
            l(15, 20, 50_00, Categoria.COMIDA_FORA, "Larissa"),
            l(18, 8, 30_00, Categoria.COMIDA_FORA, "Thiago"),
            // semana anterior (5 a 11/10)
            l(6, 9, 100_00, Categoria.MERCADO, "Thiago"),
            l(11, 20, 40_00, Categoria.COMIDA_FORA, "Larissa"));

    @Test
    void somaASemanaAtualEComparaAteOMesmoPonto() {
        ResumoCalculado r = CalculadoraResumo.calcular(Periodo.SEMANA, 0, AGORA, DADOS, 0);
        assertEquals(280_00, r.total());
        // domingo 20h da semana passada ainda não tinha chegado "no mesmo ponto" (domingo meio-dia)
        assertEquals(100_00, r.totalAnterior());
        assertTrue(r.emAndamento());
        assertEquals(156.0 / 168.0, r.fracaoDecorrida(), 0.0001);
        assertEquals(3, r.quantidade());
        assertEquals("12 a 18 de out", r.rotulo());
    }

    @Test
    void diaComparaComOntemAteAMesmaHoraEUsaOrcamentoDiario() {
        List<Lancamento> dados = List.of(
                l(18, 8, 30_00, Categoria.COMIDA_FORA, "Thiago"),
                l(17, 9, 20_00, Categoria.COMIDA_FORA, "Thiago"),
                l(17, 20, 90_00, Categoria.MERCADO, "Thiago"));
        ResumoCalculado r = CalculadoraResumo.calcular(Periodo.DIA, 0, AGORA, dados, 3100_00);
        assertEquals(30_00, r.total());
        // ontem às 20h ainda não tinha chegado "na mesma hora" (meio-dia)
        assertEquals(20_00, r.totalAnterior());
        assertEquals(100_00, r.orcamentoPeriodo());
        assertEquals(0.5, r.fracaoDecorrida(), 0.0001);
        assertEquals("Domingo, 18 de out", r.rotulo());
        assertEquals("↑ R$ 10,00 a mais que ontem até esta hora (+50%)", RespostasBot.comparacao(r));
        List<Dica> dicas = GeradorDicas.gerar(r, AGORA);
        assertEquals("Hoje ainda cabem R$ 70,00 no orçamento do dia.", dicas.get(0).texto());
    }

    @Test
    void agrupaPorCategoriaEPessoa() {
        ResumoCalculado r = CalculadoraResumo.calcular(Periodo.SEMANA, 0, AGORA, DADOS, 0);
        assertEquals(Categoria.MERCADO, r.porCategoria().get(0).categoria());
        assertEquals(71, r.porCategoria().get(0).percentual());
        assertEquals(80_00, r.porCategoria().get(1).total());
        assertEquals("Thiago", r.porPessoa().get(0).autor());
        assertEquals(230_00, r.porPessoa().get(0).total());
    }

    @Test
    void orcamentoDaSemanaEhProporcionalAoMes() {
        ResumoCalculado r = CalculadoraResumo.calcular(Periodo.SEMANA, 0, AGORA, DADOS, 3100_00);
        assertEquals(700_00, r.orcamentoPeriodo());
        ResumoCalculado mes = CalculadoraResumo.calcular(Periodo.MES, 0, AGORA, DADOS, 3100_00);
        assertEquals(3100_00, mes.orcamentoPeriodo());
        assertEquals(420_00, mes.total());
    }

    @Test
    void periodoFechadoComparaComOAnteriorInteiro() {
        ResumoCalculado r = CalculadoraResumo.calcular(Periodo.SEMANA, -1, AGORA, DADOS, 0);
        assertFalse(r.emAndamento());
        assertEquals(1.0, r.fracaoDecorrida(), 0.0);
        assertEquals(140_00, r.total());
        assertEquals(0, r.totalAnterior());
    }
}
