package co.inmobiliaria360.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name = "usuarios") @Getter @Setter
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String email;
    private String passwordHash;
    private String nombre;
    @Enumerated(EnumType.STRING) private Rol rol;
    private boolean activo = true;
    private LocalDateTime creadoEn = LocalDateTime.now();
}
