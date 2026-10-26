package br.com.caderneta.nucleo;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;

/** Intervalo de tempo fechado no início e aberto no fim: [inicio, fim). */
public record Intervalo(ZonedDateTime inicio, ZonedDateTime fim) {

    public boolean contem(Instant instante) {
        return !instante.isBefore(inicio.toInstant()) && instante.isBefore(fim.toInstant());
    }

    public Duration duracao() {
        return Duration.between(inicio, fim);
    }
}
