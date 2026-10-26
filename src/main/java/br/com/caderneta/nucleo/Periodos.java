package br.com.caderneta.nucleo;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;

/** Calcula semanas (segunda a domingo) e meses, e os nomes que aparecem para as pessoas. */
public final class Periodos {

    private static final String[] MESES = {"jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez"};
    private static final String[] MESES_LONGOS = {"janeiro", "fevereiro", "março", "abril", "maio", "junho", "julho",
            "agosto", "setembro", "outubro", "novembro", "dezembro"};

    private Periodos() {
    }

    /**
     * @param offset 0 para o período atual, -1 para o anterior, e assim por diante
     */
    public static Intervalo de(Periodo periodo, int offset, ZonedDateTime agora) {
        ZoneId zona = agora.getZone();
        LocalDate hoje = agora.toLocalDate();
        if (periodo == Periodo.SEMANA) {
            LocalDate segunda = hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks(offset);
            return new Intervalo(segunda.atStartOfDay(zona), segunda.plusWeeks(1).atStartOfDay(zona));
        }
        LocalDate primeiro = hoje.withDayOfMonth(1).plusMonths(offset);
        return new Intervalo(primeiro.atStartOfDay(zona), primeiro.plusMonths(1).atStartOfDay(zona));
    }

    /** "12 a 18 de out", "29 de set a 5 de out" ou "Outubro de 2026". */
    public static String rotulo(Periodo periodo, Intervalo intervalo) {
        LocalDate a = intervalo.inicio().toLocalDate();
        if (periodo == Periodo.MES) {
            return Texto.capitalizar(mesLongo(a)) + " de " + a.getYear();
        }
        LocalDate b = intervalo.fim().toLocalDate().minusDays(1);
        if (a.getMonth() == b.getMonth()) {
            return a.getDayOfMonth() + " a " + b.getDayOfMonth() + " de " + mesCurto(b);
        }
        return a.getDayOfMonth() + " de " + mesCurto(a) + " a " + b.getDayOfMonth() + " de " + mesCurto(b);
    }

    public static String mesCurto(LocalDate data) {
        return MESES[data.getMonthValue() - 1];
    }

    public static String mesLongo(LocalDate data) {
        return MESES_LONGOS[data.getMonthValue() - 1];
    }

    /** "17/10". */
    public static String dataCurta(LocalDate data) {
        return String.format("%02d/%02d", data.getDayOfMonth(), data.getMonthValue());
    }

    /** "14:05". */
    public static String hora(ZonedDateTime quando) {
        return String.format("%02d:%02d", quando.getHour(), quando.getMinute());
    }

    /** "hoje, 14:05", "ontem (17/10), 12:00" ou "15/10, 19:30". */
    public static String quando(Instant instante, ZonedDateTime agora) {
        ZonedDateTime z = instante.atZone(agora.getZone());
        LocalDate dia = z.toLocalDate();
        LocalDate hoje = agora.toLocalDate();
        if (dia.equals(hoje)) {
            return "hoje, " + hora(z);
        }
        if (dia.equals(hoje.minusDays(1))) {
            return "ontem (" + dataCurta(dia) + "), " + hora(z);
        }
        return dataCurta(dia) + ", " + hora(z);
    }
}
