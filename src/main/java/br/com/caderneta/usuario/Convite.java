package br.com.caderneta.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Link para outra pessoa criar a própria conta. Vale uma vez; sem data de validade, não expira. */
@Entity
@Table(name = "convite")
public class Convite {

    @Id
    @Column(name = "codigo_hash", length = 64)
    private String codigoHash;

    @Column(name = "criado_por", nullable = false, length = 30)
    private String criadoPor;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    /** Vazio = não expira. */
    @Column(name = "expira_em")
    private Instant expiraEm;

    @Column(name = "usado_por", length = 30)
    private String usadoPor;

    @Column(name = "usado_em")
    private Instant usadoEm;

    protected Convite() {
    }

    public Convite(String codigoHash, String criadoPor, Instant criadoEm, Instant expiraEm) {
        this.codigoHash = codigoHash;
        this.criadoPor = criadoPor;
        this.criadoEm = criadoEm;
        this.expiraEm = expiraEm;
    }

    public boolean usado() {
        return usadoPor != null;
    }

    public boolean expirado(Instant agora) {
        return expiraEm != null && !agora.isBefore(expiraEm);
    }

    void marcarUsado(String login, Instant agora) {
        this.usadoPor = login;
        this.usadoEm = agora;
    }

    public Instant getExpiraEm() {
        return expiraEm;
    }

    public String getCriadoPor() {
        return criadoPor;
    }
}
