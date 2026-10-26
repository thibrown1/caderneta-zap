package br.com.caderneta.gasto;

import br.com.caderneta.nucleo.Categoria;
import br.com.caderneta.nucleo.Dinheiro;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

public record GastoResponse(UUID id, long valorCentavos, String valor, String descricao, Categoria categoria,
                            String categoriaNome, String motivo, OffsetDateTime quando, String autor, Origem origem) {

    public static GastoResponse de(Gasto g, ZoneId zona) {
        return new GastoResponse(g.getId(), g.getValorCentavos(), Dinheiro.formatar(g.getValorCentavos()),
                g.getDescricao(), g.getCategoria(), g.getCategoria().nome(), g.getMotivo(),
                g.getQuando().atZone(zona).toOffsetDateTime(), g.getAutor(), g.getOrigem());
    }
}
