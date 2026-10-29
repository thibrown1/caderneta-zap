package br.com.caderneta.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Uma pessoa que entra no painel. Cada uma vê só os próprios gastos. */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @Column(length = 30)
    private String login;

    @Column(nullable = false, length = 40)
    private String nome;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    /** O dono do painel (PAINEL_USUARIO). Só ele convida outras pessoas. */
    @Column(nullable = false)
    private boolean admin;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected Usuario() {
    }

    public Usuario(String login, String nome, String senhaHash, boolean admin, Instant criadoEm) {
        this.login = login;
        this.nome = nome;
        this.senhaHash = senhaHash;
        this.admin = admin;
        this.criadoEm = criadoEm;
    }

    public String getLogin() {
        return login;
    }

    public String getNome() {
        return nome;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public boolean isAdmin() {
        return admin;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    void trocarSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    void tornarAdmin() {
        this.admin = true;
    }
}
