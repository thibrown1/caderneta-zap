package br.com.caderneta.nucleo;

import java.util.Optional;

public enum Periodo {
    SEMANA, MES;

    /** Aceita "semana", "mes", "mês", "SEMANA"... */
    public static Optional<Periodo> de(String texto) {
        return switch (Texto.normalizar(texto)) {
            case "semana", "semanal", "week" -> Optional.of(SEMANA);
            case "mes", "mensal", "month" -> Optional.of(MES);
            default -> Optional.empty();
        };
    }
}
