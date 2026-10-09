package co.inmobiliaria360.repository;

import co.inmobiliaria360.domain.CuentaCobro;
import co.inmobiliaria360.domain.EstadoDocumento;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    /** Pasa a PAGADO solo si aun no lo esta. Devuelve 0 si otro clic ya la pago (la fila queda bloqueada hasta el commit). */
    @Modifying
    @Query("update CuentaCobro c set c.estado = co.inmobiliaria360.domain.EstadoDocumento.PAGADO, c.pagadoEn = :ahora "
            + "where c.id = :id and c.estado <> co.inmobiliaria360.domain.EstadoDocumento.PAGADO")
    int marcarPagada(@Param("id") Long id, @Param("ahora") LocalDateTime ahora);

    @Query(value = "select nextval('cuenta_cobro_consecutivo')", nativeQuery = true)
    long siguienteConsecutivo();

    @Query("""
            select c from CuentaCobro c
            join fetch c.inmueble i join fetch c.propietario p
            where c.periodo = :periodo and c.estado = :estado
            order by c.consecutivo""")
    List<CuentaCobro> porPeriodoYEstado(@Param("periodo") String periodo, @Param("estado") EstadoDocumento estado);
}
