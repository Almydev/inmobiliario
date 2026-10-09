package co.inmobiliaria360.repository;

import co.inmobiliaria360.domain.Inquilino;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InquilinoRepository extends JpaRepository<Inquilino, Long> {

    Optional<Inquilino> findByDocumento(String documento);

    @Query("""
            select i from Inquilino i
            where lower(i.nombre) like lower(concat('%', :q, '%')) or i.documento like concat('%', :q, '%')
            order by i.nombre""")
    List<Inquilino> buscar(@Param("q") String q);
}
