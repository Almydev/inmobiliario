package co.inmobiliaria360.security;

import co.inmobiliaria360.domain.Rol;
import co.inmobiliaria360.domain.Usuario;
import co.inmobiliaria360.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Crea el primer administrador si la tabla de usuarios esta vacia y hay ADMIN_EMAIL y ADMIN_PASSWORD. */
@Component
public class AdminInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicial.class);

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final String email;
    private final String password;

    public AdminInicial(UsuarioRepository usuarios, PasswordEncoder encoder,
                        @Value("${app.admin.email:}") String email,
                        @Value("${app.admin.password:}") String password) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarios.count() > 0) return;
        if (email.isBlank() || password.isBlank()) {
            log.warn("No hay usuarios y no se definieron ADMIN_EMAIL / ADMIN_PASSWORD: nadie podra iniciar sesion");
            return;
        }
        Usuario u = new Usuario();
        u.setEmail(email.toLowerCase());
        u.setNombre("Administrador");
        u.setRol(Rol.ADMIN);
        u.setPasswordHash(encoder.encode(password));
        usuarios.save(u);
        log.info("Administrador inicial creado: {}", u.getEmail());
    }
}
