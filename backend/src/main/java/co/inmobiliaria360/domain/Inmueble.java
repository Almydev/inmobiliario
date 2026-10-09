package co.inmobiliaria360.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "inmuebles") @Getter @Setter
public class Inmueble {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String descripcion;
    private String direccion;
    private String ciudad;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Propietario propietario;
    @ManyToOne(fetch = FetchType.LAZY) private Inquilino inquilino;
    private BigDecimal canon;
    private BigDecimal pctAdminCobro = new BigDecimal("20");
    private BigDecimal pctAdminEgreso = new BigDecimal("10");
    private boolean activo = true;
    private LocalDateTime creadoEn = LocalDateTime.now();
}
