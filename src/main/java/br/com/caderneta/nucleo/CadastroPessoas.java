package br.com.caderneta.nucleo;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Quem pode anotar gastos pelo WhatsApp, lido de uma configuração no formato
 * {@code 5511999999999=Thiago,5511988888888=Larissa}.
 */
public final class CadastroPessoas {

    private final Map<String, String> nomesPorNumero;

    private CadastroPessoas(Map<String, String> nomesPorNumero) {
        this.nomesPorNumero = Collections.unmodifiableMap(nomesPorNumero);
    }

    public static CadastroPessoas de(String configuracao) {
        Map<String, String> m = new LinkedHashMap<>();
        if (configuracao != null) {
            for (String par : configuracao.split("[,;]")) {
                String p = par.trim();
                int separador = p.indexOf('=') > 0 ? p.indexOf('=') : p.indexOf(':');
                if (separador <= 0) {
                    continue;
                }
                String numero = Telefones.normalizar(p.substring(0, separador));
                String nome = p.substring(separador + 1).trim();
                if (!numero.isEmpty() && !nome.isEmpty()) {
                    m.put(numero, nome);
                }
            }
        }
        return new CadastroPessoas(m);
    }

    public Optional<String> nomeDe(String numero) {
        return Optional.ofNullable(nomesPorNumero.get(Telefones.normalizar(numero)));
    }

    public boolean vazio() {
        return nomesPorNumero.isEmpty();
    }

    public List<String> nomes() {
        return List.copyOf(nomesPorNumero.values());
    }
}
