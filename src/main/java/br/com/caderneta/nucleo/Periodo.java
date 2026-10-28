package br.com.caderneta.nucleo;

import java.util.Optional;

public enum Periodo {
    DIA, SEMANA, MES;

    /** Aceita "dia", "semana", "mes", "mês", "SEMANA"... */
    public static Optional<Periodo> de(String texto) {
        return switch (Texto.normalizar(texto)) {
            case "dia", "diario", "hoje", "day" -> Optional.of(DIA);
            case "semana", "semanal", "week" -> Optional.of(SEMANA);
            case "mes", "mensal", "month" -> Optional.of(MES);
            default -> Optional.empty();
        };
    }
}
