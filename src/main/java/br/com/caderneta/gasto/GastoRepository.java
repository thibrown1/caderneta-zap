package br.com.caderneta.gasto;

import br.com.caderneta.nucleo.Categoria;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GastoRepository extends JpaRepository<Gasto, UUID> {

    List<Gasto> findByDonoAndQuandoGreaterThanEqualAndQuandoLessThanOrderByQuandoDesc(String dono, Instant inicio, Instant fim);

    Optional<Gasto> findByIdAndDono(UUID id, String dono);

    Optional<Gasto> findFirstByTelefoneOrderByCriadoEmDesc(String telefone);

    List<Gasto> findTop8ByDonoOrderByQuandoDesc(String dono);

    boolean existsByWaMessageId(String waMessageId);

    /** Último gasto da pessoa com a mesma descrição, para repetir a categoria usada antes. */
    Optional<Gasto> findFirstByDonoAndDescricaoIgnoreCaseAndCategoriaNotOrderByQuandoDesc(String dono, String descricao,
                                                                                       Categoria categoria);
}
