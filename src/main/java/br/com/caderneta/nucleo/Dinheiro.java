package br.com.caderneta.nucleo;

import java.util.Locale;
import java.util.OptionalLong;
import java.util.regex.Pattern;

/**
 * Valores em reais sempre circulam como centavos ({@code long}) para não perder precisão.
 * Esta classe lê o que a pessoa digita ("45,90", "1.234,56", "R$ 45") e formata de volta ("R$ 1.234,56").
 */
public final class Dinheiro {

    /** R$ 10 milhões: acima disso é quase certo que foi erro de digitação. */
    public static final long MAXIMO_CENTAVOS = 1_000_000_000L;

    private static final Pattern VALOR = Pattern.compile(
            "^(\\d{1,3}(?:\\.\\d{3})+(?:,\\d{1,2})?|\\d{1,9}(?:[.,]\\d{1,2})?)$");

    private Dinheiro() {
    }

    /** Lê um valor positivo. Devolve vazio se o texto não for um valor válido. */
    public static OptionalLong interpretar(String texto) {
        return interpretar(texto, false);
    }

    /**
     * Lê um valor em reais.
     *
     * @param permitirZero quando {@code true}, aceita "0" (usado para remover o orçamento)
     */
    public static OptionalLong interpretar(String texto, boolean permitirZero) {
        if (texto == null) {
            return OptionalLong.empty();
        }
        String t = texto.trim().toLowerCase(Locale.ROOT).replace(" ", "").replace(" ", "");
        if (t.startsWith("r$")) {
            t = t.substring(2);
        }
        if (!VALOR.matcher(t).matches()) {
            return OptionalLong.empty();
        }
        String reais;
        String centavos;
        if (t.contains(",")) {
            String[] partes = t.replace(".", "").split(",");
            reais = partes[0];
            centavos = partes[1];
        } else if (t.contains(".")) {
            int ponto = t.lastIndexOf('.');
            String depois = t.substring(ponto + 1);
            if (depois.length() == 3) {
                reais = t.replace(".", "");
                centavos = "";
            } else {
                reais = t.substring(0, ponto);
                centavos = depois;
            }
        } else {
            reais = t;
            centavos = "";
        }
        if (reais.length() > 9) {
            return OptionalLong.empty();
        }
        long valor = Long.parseLong(reais) * 100 + Long.parseLong((centavos + "00").substring(0, 2));
        boolean valido = permitirZero ? valor >= 0 : valor > 0;
        if (!valido || valor > MAXIMO_CENTAVOS) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(valor);
    }

    /** {@code 123456} vira {@code "R$ 1.234,56"}. */
    public static String formatar(long centavos) {
        boolean negativo = centavos < 0;
        long absoluto = Math.abs(centavos);
        String reais = String.format(Locale.ROOT, "%,d", absoluto / 100).replace(',', '.');
        long resto = absoluto % 100;
        return (negativo ? "-" : "") + "R$ " + reais + "," + (resto < 10 ? "0" : "") + resto;
    }
}
