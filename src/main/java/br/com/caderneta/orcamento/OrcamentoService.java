package br.com.caderneta.orcamento;

import br.com.caderneta.nucleo.Dinheiro;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrcamentoService {

    static final String PREFIXO = "orcamento_mensal_centavos:";

    private final ConfiguracaoRepository repositorio;

    public OrcamentoService(ConfiguracaoRepository repositorio) {
        this.repositorio = repositorio;
    }

    /** Orçamento do mês da pessoa, em centavos; 0 quando não foi definido. */
    @Transactional(readOnly = true)
    public long mensal(String dono) {
        return repositorio.findById(PREFIXO + dono).map(c -> {
            try {
                return Long.parseLong(c.getValor());
            } catch (NumberFormatException e) {
                return 0L;
            }
        }).orElse(0L);
    }

    public long definir(String dono, long centavos) {
        if (centavos < 0 || centavos > Dinheiro.MAXIMO_CENTAVOS) {
            throw new IllegalArgumentException("Orçamento inválido.");
        }
        String valor = String.valueOf(centavos);
        String chave = PREFIXO + dono;
        repositorio.findById(chave).ifPresentOrElse(c -> c.setValor(valor), () -> repositorio.save(new Configuracao(chave, valor)));
        return centavos;
    }
}
