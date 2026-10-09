package co.inmobiliaria360.repository;

import co.inmobiliaria360.domain.MovimientoBanco;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovimientoBancoRepository extends JpaRepository<MovimientoBanco, Long> {

    @Query("""
            select m from MovimientoBanco m
            left join fetch m.cuentaCobro c
            left join fetch m.comprobanteEgreso e
            where m.fecha between :desde and :hasta
            order by m.fecha, m.id""")
    List<MovimientoBanco> entre(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    /** Saldo acumulado (ingresos - gastos) de todo lo anterior a la fecha. */
    @Query("select coalesce(sum(m.ingreso - m.gasto), 0) from MovimientoBanco m where m.fecha < :fecha")
    BigDecimal saldoAntesDe(@Param("fecha") LocalDate fecha);
}
