package br.com.caderneta;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Congela o tempo em domingo, 18/10/2026, meio-dia, para os testes não dependerem do dia em que rodam. */
@TestConfiguration
public class RelogioDeTeste {

    public static final ZonedDateTime AGORA = ZonedDateTime.of(2026, 10, 18, 12, 0, 0, 0, ZoneId.of("America/Sao_Paulo"));

    @Bean
    @Primary
    Clock relogioFixo() {
        return Clock.fixed(AGORA.toInstant(), AGORA.getZone());
    }
}
