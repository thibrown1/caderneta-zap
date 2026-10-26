package br.com.caderneta.gasto;

import br.com.caderneta.nucleo.Categoria;
import br.com.caderneta.nucleo.Dinheiro;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

/**
 * Corpo do POST/PUT em /api/gastos.
 *
 * @param categoria opcional: sem ela, a categoria é deduzida da descrição
 * @param quando    opcional: sem ela, vale o momento atual
 */
public record GastoRequest(
        @Positive(message = "O valor precisa ser maior que zero")
        @Max(value = Dinheiro.MAXIMO_CENTAVOS, message = "Valor alto demais")
        long valorCentavos,
        @NotBlank(message = "Diga o que foi o gasto")
        @Size(max = 120, message = "A descrição pode ter até 120 caracteres")
        String descricao,
        Categoria categoria,
        @Size(max = 200, message = "O motivo pode ter até 200 caracteres")
        String motivo,
        OffsetDateTime quando,
        @Size(max = 60, message = "O nome pode ter até 60 caracteres")
        String autor) {

    public GastoService.DadosGasto paraDados() {
        return new GastoService.DadosGasto(valorCentavos, descricao, categoria, motivo,
                quando == null ? null : quando.toInstant(), autor);
    }
}
