package br.com.caderneta.nucleo;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Utilitários de texto usados para comparar o que as pessoas digitam no WhatsApp. */
public final class Texto {

    private static final Pattern ACENTOS = Pattern.compile("\\p{M}+");
    private static final Pattern ESPACOS = Pattern.compile("\\s+");
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private Texto() {
    }

    /** Minúsculas, sem acentos e com espaços simples: {@code "  Mês  passado "} vira {@code "mes passado"}. */
    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String semAcento = ACENTOS.matcher(Normalizer.normalize(texto, Normalizer.Form.NFD)).replaceAll("");
        return ESPACOS.matcher(semAcento.toLowerCase(Locale.ROOT)).replaceAll(" ").trim();
    }

    /** Primeira letra maiúscula, o resto como veio: {@code "lanche do almoço"} vira {@code "Lanche do almoço"}. */
    public static String capitalizar(String texto) {
        if (texto == null || texto.isBlank()) {
            return texto;
        }
        String t = texto.trim();
        int primeiro = t.codePointAt(0);
        String inicio = new String(Character.toChars(primeiro)).toUpperCase(PT_BR);
        return inicio + t.substring(Character.charCount(primeiro));
    }

    /** Corta o texto no tamanho máximo sem quebrar no meio de um emoji. */
    public static String limitar(String texto, int maximo) {
        if (texto == null || texto.length() <= maximo) {
            return texto;
        }
        int fim = maximo;
        if (Character.isHighSurrogate(texto.charAt(fim - 1))) {
            fim--;
        }
        return texto.substring(0, fim).trim();
    }
}
