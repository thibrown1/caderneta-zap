package br.com.caderneta.orcamento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Pares chave/valor simples, como o orçamento do mês. */
@Entity
@Table(name = "configuracao")
public class Configuracao {

    @Id
    @Column(length = 60)
    private String chave;

    @Column(nullable = false, length = 200)
    private String valor;

    protected Configuracao() {
    }

    public Configuracao(String chave, String valor) {
        this.chave = chave;
        this.valor = valor;
    }

    public String getChave() {
        return chave;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }
}
