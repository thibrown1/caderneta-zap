package br.com.caderneta.nucleo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MensagemParserTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 18);
    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");

    private static Comando.NovoGasto gasto(String texto) {
        Comando c = MensagemParser.interpretar(texto, HOJE);
        return assertInstanceOf(Comando.NovoGasto.class, c, () -> "esperava um gasto para \"" + texto + "\", veio " + c);
    }

    @Test
    @DisplayName("45,90 lanche")
    void valorNoComeco() {
        var g = gasto("45,90 lanche");
        assertEquals(4590, g.valorCentavos());
        assertEquals("Lanche", g.descricao());
        assertNull(g.categoria());
        assertEquals("", g.motivo());
    }

    @Test
    void cifraoSeparadoEJunto() {
        assertEquals(12000, gasto("R$ 120 mercado").valorCentavos());
        assertEquals("Mercado", gasto("R$ 120 mercado").descricao());
        assertEquals(4590, gasto("R$45,90 pizza").valorCentavos());
    }

    @Test
    void milharComPonto() {
        var g = gasto("1.234,56 aluguel");
        assertEquals(123456, g.valorCentavos());
        assertEquals("Aluguel", g.descricao());
    }

    @Test
    void valorNoFim() {
        var g = gasto("lanche da tarde 12,50");
        assertEquals(1250, g.valorCentavos());
        assertEquals("Lanche da tarde", g.descricao());
    }

    @Test
    void motivoComPorque() {
        var g = gasto("120 mercado porque acabou tudo");
        assertEquals("Mercado", g.descricao());
        assertEquals("acabou tudo", g.motivo());
    }

    @Test
    void motivoComTraco() {
        var g = gasto("50 posto - tanque vazio");
        assertEquals("Posto", g.descricao());
        assertEquals("tanque vazio", g.motivo());
    }

    @Test
    void motivoComPorQueSeparado() {
        var g = gasto("30 presente por que era aniversário");
        assertEquals("Presente", g.descricao());
        assertEquals("era aniversário", g.motivo());
    }

    @Test
    void tagDeCategoria() {
        var g = gasto("89,90 pizza #lazer");
        assertEquals(Categoria.LAZER, g.categoria());
        assertEquals("Pizza", g.descricao());
    }

    @Test
    void ontemViraMeioDiaDeOntem() {
        var g = gasto("15 padaria ontem");
        assertEquals(Integer.valueOf(1), g.diasAtras());
        ZonedDateTime agora = ZonedDateTime.of(2026, 10, 18, 9, 0, 0, 0, SP);
        assertEquals(ZonedDateTime.of(2026, 10, 17, 12, 0, 0, 0, SP), g.quando(agora));
    }

    @Test
    void dataEHora() {
        var g = gasto("32 uber 15/10 às 19h");
        assertEquals(LocalDate.of(2026, 10, 15), g.data());
        assertEquals(LocalTime.of(19, 0), g.hora());
        assertEquals("Uber", g.descricao());
    }

    @Test
    void horaComDoisPontos() {
        var g = gasto("18 café às 08:30");
        assertEquals(LocalTime.of(8, 30), g.hora());
        assertEquals("Café", g.descricao());
    }

    @Test
    void dataNoFuturoViraAnoPassado() {
        assertEquals(LocalDate.of(2025, 12, 25), gasto("10 bala 25/12").data());
    }

    @Test
    void verboEPreposicaoNoInicio() {
        var g = gasto("gastei 50 no posto");
        assertEquals(5000, g.valorCentavos());
        assertEquals("Posto", g.descricao());
    }

    @Test
    void palavraReaisDepoisDoValor() {
        var g = gasto("45 reais lanche");
        assertEquals(4500, g.valorCentavos());
        assertEquals("Lanche", g.descricao());
    }

    @Test
    void numeroNaDescricaoQuandoValorVemPrimeiro() {
        var g = gasto("25 2 pães e leite");
        assertEquals(2500, g.valorCentavos());
        assertEquals("2 pães e leite", g.descricao());
    }

    @Test
    void semDataUsaOMomentoDaMensagem() {
        ZonedDateTime agora = ZonedDateTime.of(2026, 10, 18, 9, 41, 33, 0, SP);
        assertEquals(agora.withSecond(0), gasto("10 café").quando(agora));
    }

    @Test
    void soValorEhIncompleto() {
        var c = assertInstanceOf(Comando.Incompleto.class, MensagemParser.interpretar("45", HOJE));
        assertEquals(4500, c.valorCentavos());
    }

    @Test
    void textoSemValorNaoEhGasto() {
        assertInstanceOf(Comando.NaoEntendi.class, MensagemParser.interpretar("comprei pão", HOJE));
    }

    @Test
    void comandos() {
        assertEquals(new Comando.Resumo(Periodo.SEMANA, 0), MensagemParser.interpretar("Semana", HOJE));
        assertEquals(new Comando.Resumo(Periodo.MES, 0), MensagemParser.interpretar("mês", HOJE));
        assertEquals(new Comando.Resumo(Periodo.MES, -1), MensagemParser.interpretar("Mês passado", HOJE));
        assertInstanceOf(Comando.Ajuda.class, MensagemParser.interpretar("AJUDA!", HOJE));
        assertInstanceOf(Comando.ApagarUltimo.class, MensagemParser.interpretar("apagar", HOJE));
        assertInstanceOf(Comando.Dicas.class, MensagemParser.interpretar("dicas", HOJE));
        assertInstanceOf(Comando.Hoje.class, MensagemParser.interpretar("hoje", HOJE));
        assertInstanceOf(Comando.Ultimos.class, MensagemParser.interpretar("extrato", HOJE));
        assertInstanceOf(Comando.Site.class, MensagemParser.interpretar("site", HOJE));
    }

    @Test
    void orcamento() {
        assertEquals(new Comando.DefinirOrcamento(300000), MensagemParser.interpretar("orçamento 3000", HOJE));
        assertEquals(new Comando.DefinirOrcamento(350000), MensagemParser.interpretar("Orçamento R$ 3.500,00", HOJE));
        assertEquals(new Comando.DefinirOrcamento(250000), MensagemParser.interpretar("meta: 2500", HOJE));
        assertEquals(new Comando.DefinirOrcamento(0), MensagemParser.interpretar("orçamento 0", HOJE));
    }
}
