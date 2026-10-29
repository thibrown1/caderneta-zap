package br.com.caderneta.usuario;

/** Link de convite que não existe, já foi usado ou venceu. */
public class ConviteInvalidoException extends RuntimeException {

    public ConviteInvalidoException(String mensagem) {
        super(mensagem);
    }
}
