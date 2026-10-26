package br.com.caderneta.nucleo;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Transforma a mensagem do WhatsApp em um {@link Comando}.
 *
 * <p>Formatos de gasto aceitos (o valor pode vir no começo ou no fim):
 * <pre>
 *   45,90 lanche
 *   R$ 120 mercado porque acabou tudo
 *   89,90 pizza #lazer ontem
 *   32 uber 15/10 às 19h
 *   gastei 50 no posto - tanque vazio
 * </pre>
 */
public final class MensagemParser {

    private static final Pattern DATA = Pattern.compile("^(\\d{1,2})/(\\d{1,2})(?:/(\\d{2}|\\d{4}))?$");
    private static final Pattern HORA = Pattern.compile("^(\\d{1,2})(?::(\\d{2})|h(\\d{2})?)$");
    private static final Pattern SO_NUMERO = Pattern.compile("^\\d{1,2}$");
    private static final Pattern ORCAMENTO = Pattern.compile(
            "^(?:orcamento|meta|limite)(?: do mes| mensal)?(?: de| e| =|:| para| pra)?\\s*(.+)$");
    private static final Pattern PONTUACAO_FINAL = Pattern.compile("[!?.]+$");

    private static final Set<String> MOEDA = Set.of("reais", "real", "conto", "contos", "pila", "pilas");
    private static final Set<String> SEPARADORES = Set.of("-", "–", "—", "porque", "pq", "pois", "motivo", "motivo:");
    private static final Set<String> VERBOS_INICIAIS = Set.of("gastei", "paguei", "gasto", "foi", "foram");
    private static final Set<String> PREPOSICOES = Set.of("no", "na", "nos", "nas", "com", "em", "de", "do", "da", "num", "numa");

    private MensagemParser() {
    }

    public static Comando interpretar(String mensagem, LocalDate hoje) {
        if (mensagem == null || mensagem.isBlank()) {
            return new Comando.NaoEntendi("");
        }
        String n = PONTUACAO_FINAL.matcher(Texto.normalizar(mensagem)).replaceAll("").trim();

        Comando comando = comandoFixo(n);
        if (comando != null) {
            return comando;
        }

        Matcher orc = ORCAMENTO.matcher(n);
        if (orc.matches()) {
            OptionalLong valor = Dinheiro.interpretar(orc.group(1).replace(" ", ""), true);
            if (valor.isPresent()) {
                return new Comando.DefinirOrcamento(valor.getAsLong());
            }
        }

        return gasto(mensagem.trim(), hoje);
    }

    private static Comando comandoFixo(String n) {
        return switch (n) {
            case "ajuda", "menu", "oi", "ola", "help", "comandos", "inicio", "start", "bom dia", "boa tarde",
                 "boa noite", "e ai", "opa" -> new Comando.Ajuda();
            case "semana", "resumo", "resumo da semana", "gastos da semana", "quanto gastei", "quanto gastei na semana",
                 "essa semana", "esta semana", "total" -> new Comando.Resumo(Periodo.SEMANA, 0);
            case "semana passada", "resumo da semana passada" -> new Comando.Resumo(Periodo.SEMANA, -1);
            case "mes", "resumo do mes", "gastos do mes", "quanto gastei no mes", "esse mes", "este mes", "orcamento",
                 "meta" -> new Comando.Resumo(Periodo.MES, 0);
            case "mes passado", "resumo do mes passado" -> new Comando.Resumo(Periodo.MES, -1);
            case "hoje", "gastos de hoje", "gastei hoje", "quanto gastei hoje" -> new Comando.Hoje();
            case "extrato", "ultimos", "lista", "ultimos gastos", "historico" -> new Comando.Ultimos();
            case "apagar", "apaga", "desfazer", "desfaz", "apagar ultimo", "apagar o ultimo", "cancelar", "cancela",
                 "excluir", "errei" -> new Comando.ApagarUltimo();
            case "dica", "dicas", "como economizar", "onde economizar" -> new Comando.Dicas();
            case "site", "painel", "link" -> new Comando.Site();
            default -> null;
        };
    }

    private static Comando gasto(String mensagem, LocalDate hoje) {
        List<String> tokens = new ArrayList<>(Arrays.asList(mensagem.split("\\s+")));
        boolean[] usado = new boolean[tokens.size()];

        long valor = -1;
        int[] achado = tentarValor(tokens, 0);
        if (achado != null) {
            valor = achado[1] == 2 ? valorEm(tokens.get(1)) : valorEm(tokens.get(0));
            marcar(usado, 0, achado[1]);
        } else {
            for (int i = tokens.size() - 1; i >= 0 && valor < 0; i--) {
                if (i > 0 && "as".equals(Texto.normalizar(tokens.get(i - 1)))) {
                    continue;
                }
                int[] t = tentarValor(tokens, i);
                if (t != null) {
                    valor = t[1] == 2 ? valorEm(tokens.get(i + 1)) : valorEm(tokens.get(i));
                    marcar(usado, i, t[1]);
                }
            }
        }
        if (valor < 0) {
            return new Comando.NaoEntendi(mensagem);
        }
        // "45 reais lanche": a palavra da moeda depois do valor não faz parte da descrição.
        for (int i = 0; i < tokens.size(); i++) {
            if (!usado[i] && MOEDA.contains(Texto.normalizar(tokens.get(i))) && i > 0 && usado[i - 1]) {
                usado[i] = true;
            }
        }

        Categoria categoria = null;
        Integer diasAtras = null;
        LocalDate data = null;
        LocalTime hora = null;
        List<String> resto = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {
            if (usado[i]) {
                continue;
            }
            String t = tokens.get(i);
            String tn = limparPontuacao(Texto.normalizar(t));
            String proximo = i + 1 < tokens.size() ? limparPontuacao(Texto.normalizar(tokens.get(i + 1))) : "";
            boolean depoisDeAs = i > 0 && "as".equals(Texto.normalizar(tokens.get(i - 1)));

            if (t.startsWith("#") && t.length() > 1) {
                var tag = Categoria.porApelido(t);
                if (tag.isPresent()) {
                    categoria = tag.get();
                } else {
                    resto.add(t.substring(1));
                }
                continue;
            }
            switch (tn) {
                case "ontem" -> {
                    diasAtras = 1;
                    continue;
                }
                case "anteontem" -> {
                    diasAtras = 2;
                    continue;
                }
                case "hoje" -> {
                    diasAtras = 0;
                    continue;
                }
                default -> {
                }
            }
            Matcher md = DATA.matcher(tn);
            if (md.matches()) {
                LocalDate d = data(md, hoje);
                if (d != null) {
                    data = d;
                    continue;
                }
            }
            if (("as".equals(tn) || "a".equals(tn)) && (HORA.matcher(proximo).matches()
                    || ("as".equals(tn) && SO_NUMERO.matcher(proximo).matches()))) {
                continue;
            }
            Matcher mh = HORA.matcher(tn);
            if (mh.matches()) {
                LocalTime h = hora(mh.group(1), mh.group(2) != null ? mh.group(2) : mh.group(3));
                if (h != null) {
                    hora = h;
                    continue;
                }
            }
            if (depoisDeAs && SO_NUMERO.matcher(tn).matches()) {
                LocalTime h = hora(tn, null);
                if (h != null) {
                    hora = h;
                    continue;
                }
            }
            resto.add(t);
        }

        int separador = -1;
        int pular = 1;
        for (int i = 0; i < resto.size(); i++) {
            String tn = Texto.normalizar(resto.get(i));
            if (SEPARADORES.contains(tn)) {
                separador = i;
                break;
            }
            if ("por".equals(tn) && i + 1 < resto.size() && "que".equals(Texto.normalizar(resto.get(i + 1)))) {
                separador = i;
                pular = 2;
                break;
            }
        }
        List<String> partesDescricao = separador < 0 ? resto : resto.subList(0, separador);
        List<String> partesMotivo = separador < 0 ? List.of() : resto.subList(Math.min(resto.size(), separador + pular), resto.size());

        String descricao = Texto.limitar(limparBordas(String.join(" ", tirarInicio(partesDescricao))), 120);
        String motivo = Texto.limitar(limparBordas(String.join(" ", partesMotivo)), 200);

        if (descricao.isEmpty()) {
            return new Comando.Incompleto(valor);
        }
        return new Comando.NovoGasto(valor, Texto.capitalizar(descricao), categoria, motivo, diasAtras, data, hora);
    }

    /** Devolve {@code [posição, quantidadeDeTokens]} se houver um valor em reais começando em {@code i}. */
    private static int[] tentarValor(List<String> tokens, int i) {
        String t = tokens.get(i);
        if ("r$".equalsIgnoreCase(t) && i + 1 < tokens.size() && valorEm(tokens.get(i + 1)) > 0) {
            return new int[] {i, 2};
        }
        if (valorEm(t) > 0) {
            return new int[] {i, 1};
        }
        return null;
    }

    private static long valorEm(String token) {
        String t = token.replaceAll("[;:!?]+$", "");
        if (t.endsWith(",") || t.endsWith(".")) {
            t = t.substring(0, t.length() - 1);
        }
        OptionalLong v = Dinheiro.interpretar(t);
        return v.isPresent() ? v.getAsLong() : -1;
    }

    private static void marcar(boolean[] usado, int inicio, int quantidade) {
        for (int k = inicio; k < inicio + quantidade && k < usado.length; k++) {
            usado[k] = true;
        }
    }

    private static List<String> tirarInicio(List<String> partes) {
        int i = 0;
        while (i < partes.size() && VERBOS_INICIAIS.contains(Texto.normalizar(partes.get(i)))) {
            i++;
        }
        while (i < partes.size() && PREPOSICOES.contains(Texto.normalizar(partes.get(i)))) {
            i++;
        }
        return partes.subList(i, partes.size());
    }

    private static String limparPontuacao(String s) {
        return s.replaceAll("[,;!?]+$", "");
    }

    private static String limparBordas(String s) {
        return s.replaceAll("^[\\s,;:\\-–—]+", "").replaceAll("[\\s,;:\\-–—]+$", "").trim();
    }

    private static LocalDate data(Matcher m, LocalDate hoje) {
        try {
            int dia = Integer.parseInt(m.group(1));
            int mes = Integer.parseInt(m.group(2));
            String anoTexto = m.group(3);
            Integer ano = anoTexto == null ? null
                    : anoTexto.length() == 2 ? 2000 + Integer.parseInt(anoTexto) : Integer.parseInt(anoTexto);
            LocalDate d = LocalDate.of(ano == null ? hoje.getYear() : ano, mes, dia);
            if (ano == null && d.isAfter(hoje)) {
                d = d.minusYears(1);
            }
            return d;
        } catch (DateTimeException e) {
            return null;
        }
    }

    private static LocalTime hora(String h, String m) {
        int hh = Integer.parseInt(h);
        int mm = m == null ? 0 : Integer.parseInt(m);
        if (hh > 23 || mm > 59) {
            return null;
        }
        return LocalTime.of(hh, mm);
    }
}
