package br.com.caderneta.whatsapp;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * A Meta pode entregar a mesma mensagem mais de uma vez. Guarda os últimos IDs para não responder em dobro.
 * Gastos têm uma segunda proteção no banco (coluna wa_message_id única).
 */
@Component
public class MensagensProcessadas {

    private static final int LIMITE = 2000;

    private final Map<String, Boolean> vistas = new LinkedHashMap<>(256, 0.75f, false) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Boolean> maisAntiga) {
            return size() > LIMITE;
        }
    };

    /** {@code true} se for a primeira vez que esse ID aparece. */
    public synchronized boolean primeiraVez(String id) {
        if (id == null) {
            return true;
        }
        return vistas.putIfAbsent(id, Boolean.TRUE) == null;
    }
}
