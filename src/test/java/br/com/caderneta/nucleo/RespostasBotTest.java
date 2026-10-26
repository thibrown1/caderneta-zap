package br.com.caderneta.nucleo;

import static br.com.caderneta.nucleo.CalculadoraResumoTest.AGORA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class RespostasBotTest {

    private static final ResumoCalculado SEMANA =
            CalculadoraResumo.calcular(Periodo.SEMANA, 0, AGORA, CalculadoraResumoTest.DADOS, 0);

    @Test
    void confirmaOGastoAnotado() {
        Lancamento l = new Lancamento("1", 45_90, Categoria.COMIDA_FORA, AGORA.toInstant(), "Thiago", "Lanche", "sem tempo");
        String r = RespostasBot.gastoAnotado(l, AGORA, SEMANA);
        assertTrue(r.startsWith("✅ Anotado: *R$ 45,90* em Lanche"), r);
        assertTrue(r.contains("Categoria: Comida fora"), r);
        assertTrue(r.contains("Motivo: sem tempo"), r);
        assertTrue(r.contains("Semana até agora: *R$ 280,00*"), r);
        assertFalse(r.contains("Data:"), "gasto de hoje não precisa mostrar a data");
    }

    @Test
    void mostraADataQuandoNaoEhHoje() {
        Lancamento l = new Lancamento("1", 10_00, Categoria.OUTROS,
                ZonedDateTime.of(2026, 10, 17, 12, 0, 0, 0, AGORA.getZone()).toInstant(), "Thiago", "Coisa", "");
        String r = RespostasBot.gastoAnotado(l, AGORA, SEMANA);
        assertTrue(r.contains("Data: ontem (17/10), 12:00"), r);
        assertTrue(r.contains("#mercado"), "categoria Outros sugere usar tag");
    }

    @Test
    void resumoDaSemana() {
        String r = RespostasBot.resumo(SEMANA, List.of(), "https://caderneta.duckdns.org");
        assertTrue(r.startsWith("📊 *Semana · 12 a 18 de out*"), r);
        assertTrue(r.contains("Total: *R$ 280,00* (3 gastos)"), r);
        assertTrue(r.contains("↑ R$ 180,00 a mais que no mesmo ponto da semana passada (+180%)"), r);
        assertTrue(r.contains("• Mercado: R$ 200,00 (71%)"), r);
        assertTrue(r.contains("*Por pessoa*"), r);
        assertTrue(r.endsWith("Painel completo: https://caderneta.duckdns.org"), r);
    }

    @Test
    void negritaValoresUmaVezSo() {
        assertEquals("custa *R$ 1.234,56* hoje", RespostasBot.negritarValores("custa R$ 1.234,56 hoje"));
        assertEquals("já *R$ 5,00*", RespostasBot.negritarValores("já *R$ 5,00*"));
    }
}
