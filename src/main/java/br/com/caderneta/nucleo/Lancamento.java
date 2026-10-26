package br.com.caderneta.nucleo;

import java.time.Instant;

/** Visão de um gasto usada nos cálculos, independente de banco de dados. */
public record Lancamento(String id, long valorCentavos, Categoria categoria, Instant quando,
                         String autor, String descricao, String motivo) {

    public boolean temMotivo() {
        return motivo != null && !motivo.isBlank();
    }
}
