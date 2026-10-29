package br.com.caderneta.gasto;

import br.com.caderneta.nucleo.Categoria;
import br.com.caderneta.nucleo.Lancamento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "gasto")
public class Gasto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "valor_centavos", nullable = false)
    private long valorCentavos;

    @Column(nullable = false, length = 120)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Categoria categoria;

    @Column(length = 200)
    private String motivo;

    @Column(nullable = false)
    private Instant quando;

    @Column(length = 60)
    private String autor;

    /** Login da conta dona do gasto. Cada pessoa só vê os próprios. */
    @Column(nullable = false, length = 30)
    private String dono;

    /** Telefone de quem anotou pelo WhatsApp, já normalizado. Vazio quando veio do site. */
    @Column(length = 20)
    private String telefone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Origem origem;

    /** ID da mensagem na Meta. É único: se a Meta reenviar a mesma mensagem, o gasto não duplica. */
    @Column(name = "wa_message_id", length = 100, unique = true)
    private String waMessageId;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em")
    private Instant atualizadoEm;

    protected Gasto() {
    }

    public Gasto(String dono, long valorCentavos, String descricao, Categoria categoria, String motivo, Instant quando,
                 String autor, String telefone, Origem origem, String waMessageId, Instant criadoEm) {
        this.dono = dono;
        this.valorCentavos = valorCentavos;
        this.descricao = descricao;
        this.categoria = categoria;
        this.motivo = motivo;
        this.quando = quando;
        this.autor = autor;
        this.telefone = telefone;
        this.origem = origem;
        this.waMessageId = waMessageId;
        this.criadoEm = criadoEm;
    }

    public void atualizar(long valorCentavos, String descricao, Categoria categoria, String motivo, Instant quando,
                          String autor, Instant agora) {
        this.valorCentavos = valorCentavos;
        this.descricao = descricao;
        this.categoria = categoria;
        this.motivo = motivo;
        this.quando = quando;
        this.autor = autor;
        this.atualizadoEm = agora;
    }

    public Lancamento paraLancamento() {
        return new Lancamento(id == null ? null : id.toString(), valorCentavos, categoria, quando, autor, descricao, motivo);
    }

    public UUID getId() {
        return id;
    }

    public long getValorCentavos() {
        return valorCentavos;
    }

    public String getDescricao() {
        return descricao;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public String getMotivo() {
        return motivo;
    }

    public Instant getQuando() {
        return quando;
    }

    public String getAutor() {
        return autor;
    }

    public String getDono() {
        return dono;
    }

    public String getTelefone() {
        return telefone;
    }

    public Origem getOrigem() {
        return origem;
    }

    public String getWaMessageId() {
        return waMessageId;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
