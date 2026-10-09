package co.inmobiliaria360.repository;

import co.inmobiliaria360.domain.CuentaCobro;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CuentaCobroRepository extends JpaRepository<CuentaCobro, Long> {

    boolean existsByInmuebleIdAndPeriodo(Long inmuebleId, String periodo);

    @Query("""
            select c from CuentaCobro c
            join fetch c.inmueble i join fetch c.inquilino t join fetch c.propietario p
            where (:periodo = '' or c.periodo = :periodo)
              and (lower(t.nombre) like lower(concat('%', :q, '%'))
                   or lower(i.descripcion) like lower(concat('%', :q, '%'))
                   or lower(p.nombre) like lower(concat('%', :q, '%')))
            order by c.consecutivo desc""")
    List<CuentaCobro> buscar(@Param("q") String q, @Param("periodo") String periodo);

    @Query("""
            select c from CuentaCobro c
            join fetch c.inmueble i join fetch c.inquilino t join fetch c.propietario p
            where c.id = :id""")
    Optional<CuentaCobro> detalle(@Param("id") Long id);

    @Query(value = "select nextval('cuenta_cobro_consecutivo')", nativeQuery = true)
    long siguienteConsecutivo();
}
