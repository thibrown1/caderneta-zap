package br.com.caderneta.usuario;

import br.com.caderneta.config.CadernetaProperties;
import br.com.caderneta.nucleo.Texto;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Contas do painel. O dono (PAINEL_USUARIO / PAINEL_SENHA) é criado sozinho na subida;
 * as outras pessoas entram por um link de convite que o dono gera.
 */
@Service
@Transactional
public class UsuarioService implements UserDetailsService {

    private static final Pattern LOGIN = Pattern.compile("[a-z0-9._-]{3,30}");
    static final int SENHA_MINIMA = 6;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    public record NovoConvite(String codigo) {
    }

    private final UsuarioRepository usuarios;
    private final ConviteRepository convites;
    private final PasswordEncoder senhas;
    private final CadernetaProperties props;
    private final Clock clock;

    public UsuarioService(UsuarioRepository usuarios, ConviteRepository convites, PasswordEncoder senhas,
                          CadernetaProperties props, Clock clock) {
        this.usuarios = usuarios;
        this.convites = convites;
        this.senhas = senhas;
        this.props = props;
        this.clock = clock;
    }

    public String loginDono() {
        return normalizar(props.usuario());
    }

    /** Cria o dono do painel na primeira subida e acompanha a troca de PAINEL_SENHA. */
    public void garantirDono() {
        if (!StringUtils.hasText(props.senha())) {
            throw new IllegalStateException("Defina a senha do painel na variável PAINEL_SENHA.");
        }
        String login = loginDono();
        usuarios.findById(login).ifPresentOrElse(u -> {
            // Só regrava o hash quando a senha mudou: regravar sempre derrubaria o "continuar conectado" a cada reinício.
            if (!senhas.matches(props.senha(), u.getSenhaHash())) {
                u.trocarSenhaHash(senhas.encode(props.senha()));
            }
            if (!u.isAdmin()) {
                u.tornarAdmin();
            }
        }, () -> usuarios.save(new Usuario(login, Texto.capitalizar(login), senhas.encode(props.senha()), true, Instant.now(clock))));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        Usuario u = usuarios.findById(normalizar(username))
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
        return User.withUsername(u.getLogin())
                .password(u.getSenhaHash())
                .roles(u.isAdmin() ? new String[] {"USUARIO", "ADMIN"} : new String[] {"USUARIO"})
                .build();
    }

    @Transactional(readOnly = true)
    public Optional<Usuario> buscar(String login) {
        return usuarios.findById(normalizar(login));
    }

    /** Nome para mostrar. Se a conta não existir (testes), usa o próprio login. */
    @Transactional(readOnly = true)
    public String nomeDe(String login) {
        return buscar(login).map(Usuario::getNome).orElseGet(() -> Texto.capitalizar(normalizar(login)));
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return usuarios.findAllByOrderByCriadoEmAsc();
    }

    /**
     * De quem é um gasto que chegou pelo WhatsApp: da conta cujo nome ou login bate com o nome
     * cadastrado em PESSOAS para aquele número. Sem conta correspondente, fica com o dono.
     */
    @Transactional(readOnly = true)
    public String loginPorNome(String nome) {
        String procurado = Texto.normalizar(nome == null ? "" : nome);
        if (procurado.isEmpty()) {
            return loginDono();
        }
        return usuarios.findAll().stream()
                .filter(u -> u.getLogin().equals(procurado) || Texto.normalizar(u.getNome()).equals(procurado)
                        || Texto.normalizar(u.getNome()).split(" ")[0].equals(procurado))
                .map(Usuario::getLogin)
                .findFirst()
                .orElseGet(this::loginDono);
    }

    public NovoConvite criarConvite(String criadoPor) {
        byte[] bytes = new byte[24];
        ALEATORIO.nextBytes(bytes);
        String codigo = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        // O link não expira: só deixa de valer depois que alguém cria a conta com ele.
        convites.save(new Convite(hash(codigo), normalizar(criadoPor), Instant.now(clock), null));
        return new NovoConvite(codigo);
    }

    @Transactional(readOnly = true)
    public Convite conviteValido(String codigo) {
        Convite convite = codigo == null || codigo.isBlank() ? null : convites.findById(hash(codigo.trim())).orElse(null);
        if (convite == null) {
            throw new ConviteInvalidoException("Esse link de convite não existe. Confira se ele foi copiado inteiro.");
        }
        if (convite.usado()) {
            throw new ConviteInvalidoException("Esse link já foi usado. Peça um link novo para quem te convidou.");
        }
        if (convite.expirado(Instant.now(clock))) {
            throw new ConviteInvalidoException("Esse link venceu. Peça um link novo para quem te convidou.");
        }
        return convite;
    }

    public Usuario aceitarConvite(String codigo, String nome, String login, String senha) {
        Convite convite = conviteValido(codigo);
        Usuario novo = novaConta(nome, login, senha);
        convite.marcarUsado(novo.getLogin(), Instant.now(clock));
        return novo;
    }

    /** O dono cria a conta de outra pessoa direto, sem link. */
    public Usuario criarConta(String nome, String login, String senha) {
        return novaConta(nome, login, senha);
    }

    /** Cada pessoa troca a própria senha, confirmando a atual. */
    public void trocarSenha(String login, String atual, String nova) {
        Usuario u = usuarios.findById(normalizar(login))
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada."));
        if (atual == null || !senhas.matches(atual, u.getSenhaHash())) {
            throw new IllegalArgumentException("A senha atual não confere.");
        }
        validarSenha(nova);
        u.trocarSenhaHash(senhas.encode(nova));
    }

    private Usuario novaConta(String nome, String login, String senha) {
        String nomeLimpo = nome == null ? "" : nome.trim().replaceAll("\\s+", " ");
        if (nomeLimpo.isEmpty() || nomeLimpo.length() > 40) {
            throw new IllegalArgumentException("Coloque o nome, com até 40 letras.");
        }
        String loginLimpo = normalizar(login);
        if (!LOGIN.matcher(loginLimpo).matches()) {
            throw new IllegalArgumentException("O usuário precisa ter de 3 a 30 letras ou números, sem espaço nem acento.");
        }
        validarSenha(senha);
        if (usuarios.existsById(loginLimpo)) {
            throw new IllegalArgumentException("Esse usuário já existe. Escolha outro.");
        }
        return usuarios.save(new Usuario(loginLimpo, nomeLimpo, senhas.encode(senha), false, Instant.now(clock)));
    }

    private static void validarSenha(String senha) {
        if (senha == null || senha.length() < SENHA_MINIMA) {
            throw new IllegalArgumentException("A senha precisa ter pelo menos " + SENHA_MINIMA + " caracteres.");
        }
        if (senha.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("A senha pode ter até 72 caracteres.");
        }
    }

    static String normalizar(String login) {
        return login == null ? "" : login.trim().toLowerCase(Locale.ROOT);
    }

    static String hash(String codigo) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(codigo.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
