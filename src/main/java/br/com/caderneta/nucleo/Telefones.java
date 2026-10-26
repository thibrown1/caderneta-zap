package br.com.caderneta.nucleo;

/**
 * O WhatsApp às vezes entrega celulares brasileiros sem o nono dígito (551199998888 em vez de 5511999998888).
 * Normalizar evita que o mesmo número seja tratado como duas pessoas diferentes.
 */
public final class Telefones {

    private Telefones() {
    }

    public static String normalizar(String numero) {
        if (numero == null) {
            return "";
        }
        String digitos = numero.replaceAll("\\D", "");
        if (digitos.length() == 12 && digitos.startsWith("55") && "6789".indexOf(digitos.charAt(4)) >= 0) {
            digitos = digitos.substring(0, 4) + "9" + digitos.substring(4);
        }
        return digitos;
    }

    public static boolean mesmoNumero(String a, String b) {
        String na = normalizar(a);
        return !na.isEmpty() && na.equals(normalizar(b));
    }
}
