package br.com.caderneta.gasto;

import br.com.caderneta.nucleo.Categoria;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GastoRepository extends JpaRepository<Gasto, UUID> {

    List<Gasto> findByQuandoGreaterThanEqualAndQuandoLessThanOrderByQuandoDesc(Instant inicio, Instant fim);

    Optional<Gasto> findFirstByTelefoneOrderByCriadoEmDesc(String telefone);

    List<Gasto> findTop8ByOrderByQuandoDesc();

    boolean existsByWaMessageId(String waMessageId);

    /** Último gasto com a mesma descrição, para repetir a categoria usada antes. */
    Optional<Gasto> findFirstByDescricaoIgnoreCaseAndCategoriaNotOrderByQuandoDesc(String descricao, Categoria categoria);
}
