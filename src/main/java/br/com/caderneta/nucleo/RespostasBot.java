package br.com.caderneta.nucleo;

import static br.com.caderneta.nucleo.Dinheiro.formatar;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.regex.Pattern;

/** Textos que o bot responde no WhatsApp. No WhatsApp, *texto* aparece em negrito. */
public final class RespostasBot {

    private static final Pattern VALOR = Pattern.compile("(?<!\\*)R\\$ \\d{1,3}(?:\\.\\d{3})*,\\d{2}(?!\\*)");

    private RespostasBot() {
    }

    public static String ajuda(String urlSite) {
        return """
                Oi! Eu sou o *GastosXP* 💸
                Pra anotar um gasto, manda o valor e o que foi:

                • 45,90 lanche
                • 120 mercado porque acabou tudo
                • 89,90 pizza #lazer ontem
                • 32 uber 15/10 às 19h

                *Comandos*
                • *semana* ou *mês*: resumo do período
                • *hoje*: gastos de hoje
                • *extrato*: últimos lançamentos
                • *apagar*: desfaz o último que você anotou
                • *dicas*: onde dá pra economizar
                • *orçamento 3000*: define o limite do mês
                • *site*: link do painel

                Categorias pra usar com #: mercado, comida, transporte, casa, saude, lazer, compras, assinaturas, outros.
                """.strip() + rodapeSite(urlSite);
    }

    public static String gastoAnotado(Lancamento l, ZonedDateTime agora, ResumoCalculado semana) {
        StringBuilder sb = new StringBuilder();
        sb.append("✅ Anotado: *").append(formatar(l.valorCentavos())).append("* em ").append(l.descricao()).append('\n');
        sb.append("Categoria: ").append(l.categoria().nome()).append('\n');
        if (l.temMotivo()) {
            sb.append("Motivo: ").append(l.motivo()).append('\n');
        }
        if (!l.quando().atZone(agora.getZone()).toLocalDate().equals(agora.toLocalDate())) {
            sb.append("Data: ").append(Periodos.quando(l.quando(), agora)).append('\n');
        }
        sb.append("Semana até agora: *").append(formatar(semana.total())).append('*');
        if (semana.temOrcamento()) {
            sb.append(" de ").append(formatar(semana.orcamentoPeriodo()));
            if (semana.total() > semana.orcamentoPeriodo()) {
                sb.append("\n⚠️ Passou do orçamento da semana em ").append(formatar(semana.total() - semana.orcamentoPeriodo())).append('.');
            }
        }
        if (l.categoria() == Categoria.OUTROS) {
            sb.append("\n\nNão reconheci a categoria. Se quiser, apague com *apagar* e mande de novo com uma tag, tipo #mercado.");
        }
        return sb.toString();
    }

    public static String resumo(ResumoCalculado r, List<Dica> dicas, String urlSite) {
        String titulo = switch (r.periodo()) {
            case DIA -> "Dia";
            case SEMANA -> "Semana";
            case MES -> "Mês";
        } + " · " + r.rotulo();
        StringBuilder sb = new StringBuilder("📊 *").append(titulo).append("*\n");
        if (r.lancamentos().isEmpty()) {
            sb.append("Nada anotado nesse período ainda. Manda tipo: *45,90 lanche*");
            return sb.toString();
        }
        sb.append("Total: *").append(formatar(r.total())).append("* (").append(r.quantidade())
                .append(r.quantidade() == 1 ? " gasto)" : " gastos)").append('\n');
        String comparacao = comparacao(r);
        if (!comparacao.isEmpty()) {
            sb.append(comparacao).append('\n');
        }
        if (r.temOrcamento()) {
            long pct = Math.round(r.total() * 100.0 / r.orcamentoPeriodo());
            sb.append("Orçamento: ").append(formatar(r.total())).append(" de ").append(formatar(r.orcamentoPeriodo()))
                    .append(" (").append(pct).append("%)");
            if (r.total() <= r.orcamentoPeriodo()) {
                sb.append(" · sobram ").append(formatar(r.orcamentoPeriodo() - r.total()));
            } else {
                sb.append(" · passou ").append(formatar(r.total() - r.orcamentoPeriodo()));
            }
            sb.append('\n');
        }
        sb.append("\n*Por categoria*\n");
        for (var c : r.porCategoria()) {
            sb.append("• ").append(c.categoria().nome()).append(": ").append(formatar(c.total()))
                    .append(" (").append(c.percentual()).append("%)\n");
        }
        if (r.porPessoa().size() > 1) {
            sb.append("\n*Por pessoa*\n");
            for (var p : r.porPessoa()) {
                sb.append("• ").append(p.autor()).append(": ").append(formatar(p.total())).append('\n');
            }
        }
        if (!dicas.isEmpty()) {
            sb.append("\n💡 ").append(negritarValores(dicas.get(0).texto())).append('\n');
        }
        return sb.toString().strip() + rodapeSite(urlSite);
    }

    public static String comparacao(ResumoCalculado r) {
        if (r.totalAnterior() == 0) {
            return "";
        }
        String referencia = switch (r.periodo()) {
            case DIA -> r.emAndamento() ? "que ontem até esta hora" : "que no dia anterior";
            case SEMANA -> r.emAndamento() ? "que no mesmo ponto da semana passada" : "que na semana anterior";
            case MES -> r.emAndamento() ? "que no mesmo ponto do mês passado" : "que no mês anterior";
        };
        long diferenca = r.total() - r.totalAnterior();
        if (diferenca == 0) {
            return "Igual ao período anterior.";
        }
        if (diferenca < 0) {
            return "↓ " + formatar(-diferenca) + " a menos " + referencia;
        }
        return "↑ " + formatar(diferenca) + " a mais " + referencia + " (+" + Math.round(diferenca * 100.0 / r.totalAnterior()) + "%)";
    }

    public static String hoje(List<Lancamento> lancamentos, ZonedDateTime agora) {
        if (lancamentos.isEmpty()) {
            return "Nada anotado hoje ainda.";
        }
        StringBuilder sb = new StringBuilder("*Hoje* · ").append(formatar(CalculadoraResumo.soma(lancamentos))).append('\n');
        for (Lancamento l : lancamentos) {
            sb.append("• ").append(Periodos.hora(l.quando().atZone(agora.getZone()))).append(' ').append(l.descricao())
                    .append(" · ").append(formatar(l.valorCentavos())).append(autor(l)).append('\n');
        }
        return sb.toString().strip();
    }

    public static String ultimos(List<Lancamento> lancamentos, ZonedDateTime agora) {
        if (lancamentos.isEmpty()) {
            return "Ainda não tem nenhum gasto anotado. Manda tipo: *45,90 lanche*";
        }
        StringBuilder sb = new StringBuilder("*Últimos lançamentos*\n");
        for (Lancamento l : lancamentos) {
            var z = l.quando().atZone(agora.getZone());
            sb.append("• ").append(Periodos.dataCurta(z.toLocalDate())).append(' ').append(Periodos.hora(z))
                    .append(" · ").append(l.descricao()).append(" · ").append(formatar(l.valorCentavos())).append(autor(l)).append('\n');
        }
        return sb.toString().strip();
    }

    public static String apagado(Lancamento l, ZonedDateTime agora) {
        return "🗑️ Apaguei: " + formatar(l.valorCentavos()) + " em " + l.descricao() + " (" + Periodos.quando(l.quando(), agora) + ").";
    }

    public static String nadaParaApagar() {
        return "Não achei nenhum gasto seu pra apagar.";
    }

    public static String dicas(List<Dica> dicas, ResumoCalculado mes) {
        if (dicas.isEmpty()) {
            return mes.lancamentos().isEmpty()
                    ? "Ainda não tem gasto no mês pra eu analisar. Anota alguns e me pede de novo."
                    : "Nada chamando atenção no mês por enquanto. Continua anotando que eu aviso quando algo fugir do normal.";
        }
        StringBuilder sb = new StringBuilder("*Dicas do mês* (").append(mes.rotulo()).append(")\n");
        for (Dica d : dicas) {
            sb.append("\n💡 *").append(d.tipo().rotulo()).append(":* ").append(negritarValores(d.texto())).append('\n');
        }
        return sb.toString().strip();
    }

    public static String orcamentoDefinido(long mensal, ResumoCalculado mes) {
        if (mensal == 0) {
            return "Orçamento removido. Quando quiser de novo, manda *orçamento 3000*.";
        }
        long semanal = Math.round(mensal * 7.0 / mes.intervalo().inicio().toLocalDate().lengthOfMonth());
        return "Orçamento do mês: *" + formatar(mensal) + "*\nNa semana, isso dá cerca de " + formatar(semanal)
                + ".\nNo mês até agora: " + formatar(mes.total()) + ".";
    }

    public static String incompleto(long valor) {
        return "Faltou dizer o que foi esse gasto de " + formatar(valor) + ". Manda assim: *45 lanche*";
    }

    public static String naoEntendi() {
        return "Não entendi 🤔 Pra anotar, manda o valor e o que foi, tipo *45,90 lanche*. Manda *ajuda* pra ver os comandos.";
    }

    public static String soTexto() {
        return "Por enquanto eu só entendo mensagem de texto. Manda assim: *45,90 lanche*";
    }

    public static String site(String urlSite) {
        return urlSite == null || urlSite.isBlank()
                ? "O endereço do painel ainda não foi configurado no servidor (variável SITE_URL)."
                : "Painel completo: " + urlSite;
    }

    public static String numeroNaoCadastrado(String numero) {
        return "Oi! Esse número (" + numero + ") ainda não está liberado no GastosXP.\n"
                + "No servidor, coloque na variável PESSOAS assim: PESSOAS=" + numero + "=SeuNome";
    }

    /** Coloca em negrito os valores em reais que ainda não estão: "R$ 45,90" vira "*R$ 45,90*". */
    public static String negritarValores(String texto) {
        return VALOR.matcher(texto).replaceAll("*$0*");
    }

    private static String autor(Lancamento l) {
        return l.autor() == null || l.autor().isBlank() ? "" : " (" + l.autor() + ")";
    }

    private static String rodapeSite(String urlSite) {
        return urlSite == null || urlSite.isBlank() ? "" : "\n\nPainel completo: " + urlSite;
    }
}
