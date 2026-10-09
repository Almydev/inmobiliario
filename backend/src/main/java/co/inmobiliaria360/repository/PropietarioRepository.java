package co.inmobiliaria360.repository;

import co.inmobiliaria360.domain.Propietario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PropietarioRepository extends JpaRepository<Propietario, Long> {

    Optional<Propietario> findByDocumento(String documento);

    @Query("""
            select p from Propietario p
            where lower(p.nombre) like lower(concat('%', :q, '%')) or p.documento like concat('%', :q, '%')
            order by p.nombre""")
    List<Propietario> buscar(@Param("q") String q);
}
