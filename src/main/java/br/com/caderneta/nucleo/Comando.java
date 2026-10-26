package br.com.caderneta.nucleo;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;

/** O que a pessoa pediu na mensagem do WhatsApp. */
public sealed interface Comando {

    /**
     * Um gasto novo, como em "45,90 lanche porque não deu tempo".
     *
     * @param categoria  categoria escolhida com #tag, ou {@code null} para adivinhar
     * @param diasAtras  1 para "ontem", 2 para "anteontem", {@code null} se não informado
     * @param data       data escrita como 15/10, ou {@code null}
     * @param hora       hora escrita como "às 19h", ou {@code null}
     */
    record NovoGasto(long valorCentavos, String descricao, Categoria categoria, String motivo,
                     Integer diasAtras, LocalDate data, LocalTime hora) implements Comando {

        /** Resolve quando o gasto aconteceu. Sem data nem hora, vale o momento da mensagem. */
        public ZonedDateTime quando(ZonedDateTime agora) {
            LocalDate dia = data != null ? data : agora.toLocalDate().minusDays(diasAtras == null ? 0 : diasAtras);
            boolean diaInformado = data != null || (diasAtras != null && diasAtras > 0);
            LocalTime horario = hora != null ? hora
                    : diaInformado ? LocalTime.NOON : agora.toLocalTime().withSecond(0).withNano(0);
            return ZonedDateTime.of(dia, horario, agora.getZone());
        }
    }

    record Resumo(Periodo periodo, int offset) implements Comando {
    }

    record Hoje() implements Comando {
    }

    record Ultimos() implements Comando {
    }

    record ApagarUltimo() implements Comando {
    }

    record Dicas() implements Comando {
    }

    record DefinirOrcamento(long valorCentavos) implements Comando {
    }

    record Site() implements Comando {
    }

    record Ajuda() implements Comando {
    }

    /** Veio um valor, mas faltou dizer o que foi. */
    record Incompleto(long valorCentavos) implements Comando {
    }

    record NaoEntendi(String texto) implements Comando {
    }
}
