package br.com.caderneta.orcamento;

import br.com.caderneta.nucleo.Dinheiro;
import br.com.caderneta.usuario.UsuarioAtual;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orcamento")
public class OrcamentoController {

    public record OrcamentoRequest(
            @PositiveOrZero(message = "O orçamento não pode ser negativo")
            @Max(value = Dinheiro.MAXIMO_CENTAVOS, message = "Valor alto demais")
            long valorMensalCentavos) {
    }

    public record OrcamentoResponse(long valorMensalCentavos, String valorMensal) {
        static OrcamentoResponse de(long centavos) {
            return new OrcamentoResponse(centavos, Dinheiro.formatar(centavos));
        }
    }

    private final OrcamentoService servico;

    public OrcamentoController(OrcamentoService servico) {
        this.servico = servico;
    }

    @GetMapping
    public OrcamentoResponse consultar() {
        return OrcamentoResponse.de(servico.mensal(UsuarioAtual.login()));
    }

    /** 0 remove o orçamento. */
    @PutMapping
    public OrcamentoResponse definir(@Valid @RequestBody OrcamentoRequest pedido) {
        return OrcamentoResponse.de(servico.definir(UsuarioAtual.login(), pedido.valorMensalCentavos()));
    }
}
