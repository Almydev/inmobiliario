package co.inmobiliaria360.repository;

import co.inmobiliaria360.domain.Inmueble;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InmuebleRepository extends JpaRepository<Inmueble, Long> {

    @Query("""
            select i from Inmueble i
            join fetch i.propietario p
            left join fetch i.inquilino t
            where lower(i.descripcion) like lower(concat('%', :q, '%'))
               or lower(i.direccion) like lower(concat('%', :q, '%'))
               or lower(p.nombre) like lower(concat('%', :q, '%'))
               or lower(coalesce(t.nombre, '')) like lower(concat('%', :q, '%'))
            order by i.descripcion""")
    List<Inmueble> buscar(@Param("q") String q);

    long countByActivoTrueAndInquilinoIsNotNull();

    @Query("""
            select i from Inmueble i
            join fetch i.propietario
            join fetch i.inquilino
            where i.activo = true
            order by i.id""")
    List<Inmueble> activosConInquilino();
}
