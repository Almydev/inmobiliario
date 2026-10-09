package co.inmobiliaria360;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Prueba de integracion contra una base H2 en memoria (modo PostgreSQL).
 * Anula la carga del .env y fuerza la conexion de prueba: NUNCA toca Neon.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(properties = {
        "spring.config.import=optional:file:./no-existe.properties",
        "spring.datasource.url=jdbc:h2:mem:pruebas;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.defer-datasource-initialization=true",
        "spring.sql.init.mode=always",
        "app.jwt.secret=0123456789abcdef0123456789abcdef",
        "spring.datasource.hikari.maximum-pool-size=30"
})
public @interface PruebaH2 {
}
