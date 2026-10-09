package co.inmobiliaria360.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "comprobantes_egreso") @Getter @Setter
public class ComprobanteEgreso {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private Long consecutivo;
    private LocalDate fecha;
    /** Formato yyyy-MM */
    private String periodo;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Propietario propietario;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Inmueble inmueble;
    private String concepto;
    private int dias = 30;
    private BigDecimal valorBruto;
    private BigDecimal valorAdministracion = BigDecimal.ZERO;
    private BigDecimal otrosDescuentos = BigDecimal.ZERO;
    private BigDecimal totalPagado;
    private String imputacionContable;
    @Enumerated(EnumType.STRING) private EstadoDocumento estado = EstadoDocumento.BORRADOR;
    private LocalDateTime enviadoEn;
    private Long creadoPor;
    private LocalDateTime creadoEn = LocalDateTime.now();
}
