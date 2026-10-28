package br.com.caderneta.resumo;

import br.com.caderneta.config.CadernetaProperties;
import br.com.caderneta.config.WhatsAppProperties;
import br.com.caderneta.nucleo.CadastroPessoas;
import br.com.caderneta.nucleo.Categoria;
import br.com.caderneta.nucleo.Periodo;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ResumoController {

    public record CategoriaInfo(String codigo, String nome) {
    }

    public record Info(String usuario, List<String> pessoas, List<CategoriaInfo> categorias, String site,
                       boolean whatsappConfigurado) {
    }

    private final ResumoService servico;
    private final CadernetaProperties props;
    private final WhatsAppProperties whatsApp;
    private final CadastroPessoas pessoas;

    public ResumoController(ResumoService servico, CadernetaProperties props, WhatsAppProperties whatsApp, CadastroPessoas pessoas) {
        this.servico = servico;
        this.props = props;
        this.whatsApp = whatsApp;
        this.pessoas = pessoas;
    }

    /**
     * @param periodo "dia", "semana" ou "mes"
     * @param offset  0 = atual, -1 = anterior...
     */
    @GetMapping("/resumo")
    public ResumoResponse resumo(@RequestParam(defaultValue = "dia") String periodo,
                                 @RequestParam(defaultValue = "0") int offset) {
        Periodo p = Periodo.de(periodo).orElseThrow(() -> new IllegalArgumentException("Use periodo=dia, periodo=semana ou periodo=mes."));
        int limite = p == Periodo.DIA ? -730 : -120;
        if (offset > 0 || offset < limite) {
            throw new IllegalArgumentException("O offset vai de " + limite + " a 0.");
        }
        return ResumoResponse.de(servico.calcular(p, offset), props.zonaId());
    }

    /** Dados fixos que o painel usa para montar os formulários. */
    @GetMapping("/info")
    public Info info() {
        return new Info(props.usuario(), pessoas.nomes(),
                Arrays.stream(Categoria.values()).map(c -> new CategoriaInfo(c.name(), c.nome())).toList(),
                props.urlSite(), whatsApp.podeEnviar());
    }
}
