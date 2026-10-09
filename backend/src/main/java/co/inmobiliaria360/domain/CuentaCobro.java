package co.inmobiliaria360.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "cuentas_cobro") @Getter @Setter
public class CuentaCobro {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private Long consecutivo;
    private LocalDate fecha;
    /** Formato yyyy-MM */
    private String periodo;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Inmueble inmueble;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Inquilino inquilino;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Propietario propietario;
    private String concepto;
    private BigDecimal valorArriendo;
    private BigDecimal valorAdministracion = BigDecimal.ZERO;
    private BigDecimal otros = BigDecimal.ZERO;
    private BigDecimal reteFuente = BigDecimal.ZERO;
    private BigDecimal total;
    @Enumerated(EnumType.STRING) private EstadoDocumento estado = EstadoDocumento.BORRADOR;
    private LocalDateTime enviadoEn;
    private LocalDateTime pagadoEn;
    private Long creadoPor;
    private LocalDateTime creadoEn = LocalDateTime.now();
}
