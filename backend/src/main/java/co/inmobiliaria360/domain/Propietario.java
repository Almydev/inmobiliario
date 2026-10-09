package co.inmobiliaria360.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "propietarios") @Getter @Setter
public class Propietario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String nombre;
    private String documento;
    private String email;
    private String telefono;
    private String banco;
    private String tipoCuenta;
    private String numeroCuenta;
    private boolean activo = true;
    private LocalDateTime creadoEn = LocalDateTime.now();
}
