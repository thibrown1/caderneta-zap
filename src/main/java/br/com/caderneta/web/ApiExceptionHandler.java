package br.com.caderneta.web;

import br.com.caderneta.gasto.GastoNaoEncontradoException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Erros da API no formato Problem Details (RFC 9457), com mensagens em português. */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(GastoNaoEncontradoException.class)
    ProblemDetail naoEncontrado(GastoNaoEncontradoException e) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        p.setTitle("Gasto não encontrado");
        return p;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalido(MethodArgumentNotValidException e) {
        Map<String, String> erros = new LinkedHashMap<>();
        for (FieldError f : e.getBindingResult().getFieldErrors()) {
            erros.putIfAbsent(f.getField(), f.getDefaultMessage());
        }
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                erros.isEmpty() ? "Dados inválidos." : erros.values().iterator().next());
        p.setTitle("Dados inválidos");
        p.setProperty("erros", erros);
        return p;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail requisicaoInvalida(IllegalArgumentException e) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        p.setTitle("Requisição inválida");
        return p;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ProblemDetail corpoInvalido(Exception e) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Não consegui ler a requisição. Confira o JSON, os números e os códigos de categoria.");
        p.setTitle("Requisição inválida");
        return p;
    }
}
