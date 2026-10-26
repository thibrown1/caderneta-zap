package br.com.caderneta.nucleo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class UrlBancoTest {

    @Test
    void converteAConnectionStringDoNeon() {
        var c = UrlBanco.converter(
                "postgresql://neondb_owner:npg_AbC123@ep-cool-sun-a5b6c7d8-pooler.us-east-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require")
                .orElseThrow();
        assertEquals("jdbc:postgresql://ep-cool-sun-a5b6c7d8.us-east-1.aws.neon.tech/neondb?sslmode=require", c.jdbcUrl());
        assertEquals("neondb_owner", c.usuario());
        assertEquals("npg_AbC123", c.senha());
    }

    @Test
    void mantemPortaEDecodificaASenha() {
        var c = UrlBanco.converter("postgres://user:p%40ss%3Aw0rd@db.exemplo.com:6543/caderneta").orElseThrow();
        assertEquals("jdbc:postgresql://db.exemplo.com:6543/caderneta", c.jdbcUrl());
        assertEquals("p@ss:w0rd", c.senha());
    }

    @Test
    void ignoraJdbcEVazio() {
        assertTrue(UrlBanco.converter("jdbc:postgresql://db:5432/caderneta").isEmpty());
        assertTrue(UrlBanco.converter("jdbc:h2:file:./dados/caderneta").isEmpty());
        assertTrue(UrlBanco.converter(null).isEmpty());
        assertTrue(UrlBanco.converter("").isEmpty());
    }
}
