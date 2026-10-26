package br.com.caderneta.nucleo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class DinheiroTest {

    private static long ler(String texto) {
        OptionalLong v = Dinheiro.interpretar(texto);
        assertTrue(v.isPresent(), "devia ler: " + texto);
        return v.getAsLong();
    }

    @Test
    void leFormatosBrasileiros() {
        assertEquals(4500, ler("45"));
        assertEquals(4590, ler("45,9"));
        assertEquals(4590, ler("45,90"));
        assertEquals(4590, ler("45.90"));
        assertEquals(123400, ler("1.234"));
        assertEquals(123456, ler("1.234,56"));
        assertEquals(1000, ler("R$ 10"));
        assertEquals(1000, ler("r$10,00"));
    }

    @Test
    void recusaValoresInvalidos() {
        assertTrue(Dinheiro.interpretar("0").isEmpty());
        assertTrue(Dinheiro.interpretar("abc").isEmpty());
        assertTrue(Dinheiro.interpretar("45,999").isEmpty());
        assertTrue(Dinheiro.interpretar("12345678901").isEmpty());
        assertTrue(Dinheiro.interpretar("15/10").isEmpty());
        assertTrue(Dinheiro.interpretar(null).isEmpty());
    }

    @Test
    void zeroSoQuandoPermitido() {
        assertEquals(0, Dinheiro.interpretar("0", true).getAsLong());
    }

    @Test
    void formata() {
        assertEquals("R$ 1.234,56", Dinheiro.formatar(123456));
        assertEquals("R$ 0,05", Dinheiro.formatar(5));
        assertEquals("R$ 1.000.000,00", Dinheiro.formatar(100_000_000));
        assertEquals("-R$ 10,50", Dinheiro.formatar(-1050));
    }
}
