package br.com.caderneta.nucleo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class PeriodosTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");
    /** Domingo, 18/10/2026. */
    private static final ZonedDateTime AGORA = ZonedDateTime.of(2026, 10, 18, 6, 8, 0, 0, SP);

    @Test
    void semanaVaiDeSegundaADomingo() {
        Intervalo iv = Periodos.de(Periodo.SEMANA, 0, AGORA);
        assertEquals(ZonedDateTime.of(2026, 10, 12, 0, 0, 0, 0, SP), iv.inicio());
        assertEquals(ZonedDateTime.of(2026, 10, 19, 0, 0, 0, 0, SP), iv.fim());
        assertEquals("12 a 18 de out", Periodos.rotulo(Periodo.SEMANA, iv));
    }

    @Test
    void semanaQueAtravessaOMes() {
        Intervalo iv = Periodos.de(Periodo.SEMANA, -2, AGORA);
        assertEquals("28 de set a 4 de out", Periodos.rotulo(Periodo.SEMANA, iv));
    }

    @Test
    void mesAtualEAnteriores() {
        Intervalo atual = Periodos.de(Periodo.MES, 0, AGORA);
        assertEquals(ZonedDateTime.of(2026, 10, 1, 0, 0, 0, 0, SP), atual.inicio());
        assertEquals(ZonedDateTime.of(2026, 11, 1, 0, 0, 0, 0, SP), atual.fim());
        assertEquals("Outubro de 2026", Periodos.rotulo(Periodo.MES, atual));
        assertEquals("Dezembro de 2025", Periodos.rotulo(Periodo.MES, Periodos.de(Periodo.MES, -10, AGORA)));
    }

    @Test
    void descreveQuando() {
        assertEquals("hoje, 05:30", Periodos.quando(ZonedDateTime.of(2026, 10, 18, 5, 30, 0, 0, SP).toInstant(), AGORA));
        assertEquals("ontem (17/10), 12:00", Periodos.quando(ZonedDateTime.of(2026, 10, 17, 12, 0, 0, 0, SP).toInstant(), AGORA));
        assertEquals("15/10, 19:00", Periodos.quando(ZonedDateTime.of(2026, 10, 15, 19, 0, 0, 0, SP).toInstant(), AGORA));
    }
}
