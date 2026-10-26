package br.com.caderneta.nucleo;

public record Dica(Tipo tipo, String texto) {

    /** A ordem da enum é a prioridade: alertas aparecem primeiro. */
    public enum Tipo {
        ALERTA("Alerta"),
        ATENCAO("Atenção"),
        IDEIA("Ideia"),
        BOA("No caminho");

        private final String rotulo;

        Tipo(String rotulo) {
            this.rotulo = rotulo;
        }

        public String rotulo() {
            return rotulo;
        }
    }
}
