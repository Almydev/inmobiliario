package co.inmobiliaria360.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RelojConfig {

    /** Reloj de la aplicación en hora de Colombia (inyectable para poder fijar la fecha en las pruebas). */
    @Bean
    Clock reloj() {
        return Clock.system(ZoneId.of("America/Bogota"));
    }
}
