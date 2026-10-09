package co.inmobiliaria360.repository;

import co.inmobiliaria360.domain.ComprobanteEgreso;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComprobanteEgresoRepository extends JpaRepository<ComprobanteEgreso, Long> {

    boolean existsByInmuebleIdAndPeriodoAndDias(Long inmuebleId, String periodo, int dias);

    @Query("""
            select e from ComprobanteEgreso e
            join fetch e.inmueble i join fetch e.propietario p
            where (:periodo = '' or e.periodo = :periodo)
              and (lower(p.nombre) like lower(concat('%', :q, '%'))
                   or lower(i.descripcion) like lower(concat('%', :q, '%')))
            order by e.consecutivo desc""")
    List<ComprobanteEgreso> buscar(@Param("q") String q, @Param("periodo") String periodo);

    @Query("""
            select e from ComprobanteEgreso e
            join fetch e.inmueble i join fetch e.propietario p
            where e.id = :id""")
    Optional<ComprobanteEgreso> detalle(@Param("id") Long id);

    @Query(value = "select nextval('comprobante_egreso_consecutivo')", nativeQuery = true)
    long siguienteConsecutivo();
}
