package br.com.caderneta.nucleo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SegurancaEPessoasTest {

    @Test
    void normalizaCelularSemONonoDigito() {
        assertEquals("5511999998888", Telefones.normalizar("551199998888"));
        assertEquals("5511999998888", Telefones.normalizar("+55 (11) 99999-8888"));
        assertEquals("551133334444", Telefones.normalizar("551133334444"));
        assertTrue(Telefones.mesmoNumero("551199998888", "5511999998888"));
    }

    @Test
    void cadastroDePessoas() {
        CadastroPessoas p = CadastroPessoas.de("5511999998888=Thiago, 5511988887777:Larissa,lixo");
        assertEquals(Optional.of("Thiago"), p.nomeDe("551199998888"));
        assertEquals(Optional.of("Larissa"), p.nomeDe("5511988887777"));
        assertEquals(Optional.empty(), p.nomeDe("5511900000000"));
        assertEquals(List.of("Thiago", "Larissa"), p.nomes());
        assertTrue(CadastroPessoas.de("").vazio());
        assertTrue(CadastroPessoas.de(null).vazio());
    }

    @Test
    void assinaturaDoWebhook() {
        byte[] corpo = "{\"object\":\"whatsapp_business_account\"}".getBytes(StandardCharsets.UTF_8);
        String assinatura = AssinaturaWebhook.assinar(corpo, "segredo");
        assertTrue(assinatura.startsWith("sha256="));
        assertTrue(AssinaturaWebhook.valida(corpo, assinatura, "segredo"));
        assertTrue(AssinaturaWebhook.valida(corpo, assinatura.toUpperCase().replace("SHA256=", "sha256="), "segredo"));
        assertFalse(AssinaturaWebhook.valida(corpo, assinatura, "outro"));
        assertFalse(AssinaturaWebhook.valida("{}".getBytes(StandardCharsets.UTF_8), assinatura, "segredo"));
        assertFalse(AssinaturaWebhook.valida(corpo, null, "segredo"));
        assertFalse(AssinaturaWebhook.valida(corpo, "sha256=zz", "segredo"));
        assertFalse(AssinaturaWebhook.valida(corpo, assinatura, ""));
    }

    @Test
    void classificaPelaDescricao() {
        assertEquals(Optional.of(Categoria.TRANSPORTE), ClassificadorCategoria.inferir("Gasolina"));
        assertEquals(Optional.of(Categoria.COMIDA_FORA), ClassificadorCategoria.inferir("iFood sexta"));
        assertEquals(Optional.of(Categoria.COMPRAS), ClassificadorCategoria.inferir("Mercado Livre"));
        assertEquals(Optional.of(Categoria.MERCADO), ClassificadorCategoria.inferir("mercado do mês"));
        assertEquals(Optional.of(Categoria.ASSINATURAS), ClassificadorCategoria.inferir("Netflix"));
        assertEquals(Optional.of(Categoria.SAUDE), ClassificadorCategoria.inferir("Farmácia"));
        assertEquals(Optional.empty(), ClassificadorCategoria.inferir("coisa aleatória"));
        assertEquals(Optional.of(Categoria.COMIDA_FORA), Categoria.porApelido("#comida"));
        assertEquals(Optional.of(Categoria.SAUDE), Categoria.porApelido("#Saúde"));
    }
}
