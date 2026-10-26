package br.com.caderneta.gasto;

import java.util.UUID;

public class GastoNaoEncontradoException extends RuntimeException {

    public GastoNaoEncontradoException(UUID id) {
        super("Não existe gasto com o id " + id);
    }
}
