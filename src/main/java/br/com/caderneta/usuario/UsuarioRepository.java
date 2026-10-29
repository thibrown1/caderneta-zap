package br.com.caderneta.usuario;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, String> {

    List<Usuario> findAllByOrderByCriadoEmAsc();
}
