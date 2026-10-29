package br.com.caderneta.gasto;

import br.com.caderneta.config.CadernetaProperties;
import br.com.caderneta.usuario.UsuarioAtual;
import jakarta.validation.Valid;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/gastos")
public class GastoController {

    private final GastoService servico;
    private final ZoneId zona;
    private final Clock clock;

    public GastoController(GastoService servico, CadernetaProperties props, Clock clock) {
        this.servico = servico;
        this.zona = props.zonaId();
        this.clock = clock;
    }

    /** Lista os gastos entre duas datas (inclusive). Sem datas, lista o mês atual até hoje. */
    @GetMapping
    public List<GastoResponse> listar(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        LocalDate hoje = LocalDate.now(clock.withZone(zona));
        LocalDate inicio = de != null ? de : hoje.withDayOfMonth(1);
        LocalDate fim = ate != null ? ate : hoje;
        if (fim.isBefore(inicio)) {
            throw new IllegalArgumentException("A data final não pode ser antes da inicial.");
        }
        if (ChronoUnit.DAYS.between(inicio, fim) > 400) {
            throw new IllegalArgumentException("Peça no máximo 400 dias por vez.");
        }
        return servico.listar(UsuarioAtual.login(), inicio.atStartOfDay(zona).toInstant(), fim.plusDays(1).atStartOfDay(zona).toInstant())
                .stream().map(g -> GastoResponse.de(g, zona)).toList();
    }

    @GetMapping("/{id}")
    public GastoResponse buscar(@PathVariable UUID id) {
        return GastoResponse.de(servico.buscar(UsuarioAtual.login(), id), zona);
    }

    @PostMapping
    public ResponseEntity<GastoResponse> criar(@Valid @RequestBody GastoRequest pedido, UriComponentsBuilder uri) {
        Gasto gasto = servico.criarPeloSite(UsuarioAtual.login(), pedido.paraDados());
        return ResponseEntity.created(uri.path("/api/gastos/{id}").buildAndExpand(gasto.getId()).toUri())
                .body(GastoResponse.de(gasto, zona));
    }

    @PutMapping("/{id}")
    public GastoResponse atualizar(@PathVariable UUID id, @Valid @RequestBody GastoRequest pedido) {
        return GastoResponse.de(servico.atualizar(UsuarioAtual.login(), id, pedido.paraDados()), zona);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void apagar(@PathVariable UUID id) {
        servico.apagar(UsuarioAtual.login(), id);
    }
}
