package co.inmobiliaria360.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** El saldo no se guarda: se calcula acumulando ingreso - gasto por fecha e id. */
@Entity @Table(name = "movimientos_banco") @Getter @Setter
public class MovimientoBanco {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private LocalDate fecha;
    private String concepto;
    private BigDecimal administracion = BigDecimal.ZERO;
    private BigDecimal ingreso = BigDecimal.ZERO;
    private BigDecimal gasto = BigDecimal.ZERO;
    @ManyToOne(fetch = FetchType.LAZY) private CuentaCobro cuentaCobro;
    @ManyToOne(fetch = FetchType.LAZY) private ComprobanteEgreso comprobanteEgreso;
    private LocalDateTime creadoEn = LocalDateTime.now();
}
