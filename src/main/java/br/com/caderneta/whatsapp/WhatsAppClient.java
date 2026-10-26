package br.com.caderneta.whatsapp;

public interface WhatsAppClient {

    void enviarTexto(String para, String texto);

    /** Marca a mensagem como lida (os dois tracinhos azuis). */
    void marcarComoLida(String idMensagem);
}
