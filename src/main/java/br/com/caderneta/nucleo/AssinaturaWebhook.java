package br.com.caderneta.nucleo;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Confere o cabeçalho {@code X-Hub-Signature-256} que a Meta envia em cada chamada do webhook.
 * Sem isso, qualquer pessoa que descobrisse a URL poderia inserir gastos falsos.
 */
public final class AssinaturaWebhook {

    private static final String PREFIXO = "sha256=";

    private AssinaturaWebhook() {
    }

    public static boolean valida(byte[] corpo, String cabecalho, String segredoDoApp) {
        if (corpo == null || cabecalho == null || segredoDoApp == null || segredoDoApp.isBlank()
                || !cabecalho.startsWith(PREFIXO)) {
            return false;
        }
        byte[] recebido;
        try {
            recebido = HexFormat.of().parseHex(cabecalho.substring(PREFIXO.length()).trim());
        } catch (IllegalArgumentException e) {
            return false;
        }
        return MessageDigest.isEqual(hmac(corpo, segredoDoApp), recebido);
    }

    public static String assinar(byte[] corpo, String segredoDoApp) {
        return PREFIXO + HexFormat.of().formatHex(hmac(corpo, segredoDoApp));
    }

    private static byte[] hmac(byte[] corpo, String segredo) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(corpo);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 indisponível", e);
        }
    }
}
