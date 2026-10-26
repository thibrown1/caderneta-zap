package br.com.caderneta.nucleo;

import java.util.List;
import java.util.Optional;

/** Categorias de gasto. {@code cortavel} marca as que costumam ter espaço para economia. */
public enum Categoria {
    MERCADO("Mercado", false, "mercado", "supermercado", "feira"),
    COMIDA_FORA("Comida fora", true, "comida", "comidafora", "comida-fora", "delivery", "restaurante", "lanche"),
    TRANSPORTE("Transporte", false, "transporte", "carro", "uber", "gasolina", "moto"),
    CASA("Casa e contas", false, "casa", "contas", "conta", "casaecontas"),
    SAUDE("Saúde", false, "saude", "farmacia", "remedio"),
    LAZER("Lazer", true, "lazer", "passeio", "diversao", "viagem"),
    COMPRAS("Compras", true, "compras", "compra", "roupa", "roupas"),
    ASSINATURAS("Assinaturas", true, "assinaturas", "assinatura", "streaming"),
    OUTROS("Outros", false, "outros", "outro");

    private final String nome;
    private final boolean cortavel;
    private final List<String> apelidos;

    Categoria(String nome, boolean cortavel, String... apelidos) {
        this.nome = nome;
        this.cortavel = cortavel;
        this.apelidos = List.of(apelidos);
    }

    public String nome() {
        return nome;
    }

    public boolean cortavel() {
        return cortavel;
    }

    public List<String> apelidos() {
        return apelidos;
    }

    /** Encontra a categoria por um apelido, como na tag {@code #comida} ou {@code #mercado}. */
    public static Optional<Categoria> porApelido(String texto) {
        String n = Texto.normalizar(texto).replace("#", "").replace(" ", "");
        if (n.isEmpty()) {
            return Optional.empty();
        }
        for (Categoria c : values()) {
            if (c.apelidos.contains(n) || c.name().toLowerCase().replace("_", "").equals(n.replace("_", ""))) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }
}
