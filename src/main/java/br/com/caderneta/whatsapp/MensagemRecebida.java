package br.com.caderneta.whatsapp;

/**
 * @param id    ID da mensagem na Meta (wamid...)
 * @param de    número de quem mandou, como a Meta entregou (use esse mesmo para responder)
 * @param tipo  "text", "image", "audio"...
 */
public record MensagemRecebida(String id, String de, String nomePerfil, String tipo, String texto) {
}
