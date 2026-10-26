package br.com.caderneta.nucleo;

import static br.com.caderneta.nucleo.CalculadoraResumoTest.AGORA;
import static br.com.caderneta.nucleo.CalculadoraResumoTest.l;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class GeradorDicasTest {

    @Test
    void semGastosNaoHaDicas() {
        var r = CalculadoraResumo.calcular(Periodo.SEMANA, 0, AGORA, List.of(), 1000_00);
        assertTrue(GeradorDicas.gerar(r, AGORA).isEmpty());
    }

    @Test
    void orcamentoEstouradoVemPrimeiro() {
        var r = CalculadoraResumo.calcular(Periodo.SEMANA, 0, AGORA, CalculadoraResumoTest.DADOS, 500_00);
        // 500/31*7 = 112,90 de orçamento na semana; gastou 280
        List<Dica> dicas = GeradorDicas.gerar(r, AGORA);
        assertEquals(Dica.Tipo.ALERTA, dicas.get(0).tipo());
        assertTrue(dicas.get(0).texto().contains("estourou em R$ 167,10"), dicas.get(0).texto());
    }

    @Test
    void apontaCategoriaQueDisparouEDaIdeiaDeCorte() {
        List<Lancamento> dados = new ArrayList<>(CalculadoraResumoTest.DADOS);
        dados.add(l(17, 21, 120_00, Categoria.COMIDA_FORA, "Thiago"));
        dados.add(l(7, 12, 40_00, Categoria.COMIDA_FORA, "Thiago"));
        var r = CalculadoraResumo.calcular(Periodo.SEMANA, 0, AGORA, dados, 0);
        List<Dica> dicas = GeradorDicas.gerar(r, AGORA);
        // semana atual: 200 de comida fora; semana passada até domingo meio-dia: 40
        assertEquals(Dica.Tipo.ATENCAO, dicas.get(0).tipo());
        assertTrue(dicas.get(0).texto().startsWith("Comida fora subiu 400%"), dicas.get(0).texto());
        assertTrue(dicas.stream().anyMatch(d -> d.tipo() == Dica.Tipo.IDEIA && d.texto().startsWith("Comida fora levou R$ 200,00")));
        assertTrue(dicas.size() <= GeradorDicas.MAXIMO);
    }

    @Test
    void ritmoDentroDoOrcamentoEhBoaNoticia() {
        var r = CalculadoraResumo.calcular(Periodo.MES, 0, AGORA, CalculadoraResumoTest.DADOS, 3000_00);
        List<Dica> dicas = GeradorDicas.gerar(r, AGORA);
        assertTrue(dicas.stream().anyMatch(d -> d.tipo() == Dica.Tipo.BOA), dicas.toString());
    }
}
