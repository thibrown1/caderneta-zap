package br.com.caderneta.nucleo;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Adivinha a categoria pela descrição ("gasolina" é Transporte, "ifood" é Comida fora).
 * A ordem das regras importa: "mercado livre" precisa cair em Compras antes de "mercado" cair em Mercado.
 */
public final class ClassificadorCategoria {

    private record Regra(Categoria categoria, Pattern padrao) {
    }

    private static final List<Regra> REGRAS = List.of(
            regra(Categoria.ASSINATURAS, "netflix|spotify|prime video|amazon prime|disney|hbo|youtube premium|assinatura"
                    + "|icloud|globoplay|deezer|chatgpt|crunchyroll|game ?pass|paramount|apple tv|google one"),
            regra(Categoria.COMPRAS, "mercado livre|shopee|amazon|shein|aliexpress|roupa|tenis|sapato|camis|calca"
                    + "|loja|presente|eletronic|magalu|perfume|maquiagem|brinquedo"),
            regra(Categoria.MERCADO, "mercad|supermercad|feira|padaria|acougue|hortifruti|sacolao|atacad|assai"
                    + "|carrefour|pao de acucar|pao\\b|leite\\b|ovos\\b"),
            regra(Categoria.COMIDA_FORA, "ifood|lanche|lanchonete|pizza|restaurante|almoco|jantar|hamburg|burger"
                    + "|delivery|sorvet|acai|marmit|cerveja|boteco|pastel|esfiha|sushi|cafeteria|mc ?donald|starbucks"
                    + "|salgado|coxinha|refri|churrasc|espetinho|cafe\\b|cafezinho"),
            regra(Categoria.TRANSPORTE, "gasolina|combustivel|etanol|alcool\\b|posto|uber|99 ?pop|onibus|metro\\b|trem\\b"
                    + "|estacionamento|pedagio|oficina|mecanic|ipva|licenciamento|lavagem|lava rapido|bilhete unico"
                    + "|sem parar|pneu|troca de oleo|seguro do carro"),
            regra(Categoria.CASA, "aluguel|condominio|luz\\b|energia|conta de agua|agua e esgoto|sabesp|internet"
                    + "|botijao|gas\\b|conta do celular|telefone|limpeza|iptu|faxina|diarista|material de construcao"
                    + "|reforma|moveis"),
            regra(Categoria.SAUDE, "farmacia|drogaria|remedio|medic|consulta|dentista|exame|academia|plano de saude"
                    + "|psicolog|vacina|otica|oculos"),
            regra(Categoria.LAZER, "cinema|show\\b|viagem|passeio|ingresso|parque|festa|balada|hotel|pousada|bar\\b"
                    + "|steam|jogo\\b"));

    private ClassificadorCategoria() {
    }

    public static Optional<Categoria> inferir(String descricao) {
        String n = Texto.normalizar(descricao);
        if (n.isEmpty()) {
            return Optional.empty();
        }
        for (Regra r : REGRAS) {
            if (r.padrao().matcher(n).find()) {
                return Optional.of(r.categoria());
            }
        }
        return Optional.empty();
    }

    private static Regra regra(Categoria categoria, String alternativas) {
        return new Regra(categoria, Pattern.compile("\\b(?:" + alternativas + ")"));
    }
}
