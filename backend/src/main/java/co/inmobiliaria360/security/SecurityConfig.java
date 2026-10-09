package co.inmobiliaria360.security;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, UsuarioAcceso acceso) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers("/actuator/health", "/error").permitAll()
                .requestMatchers("/api/auth/me", "/api/auth/password").authenticated()
                .requestMatchers("/api/usuarios/**").hasRole("ADMIN")
                .requestMatchers("/api/**").hasAnyRole("ADMIN", "OPERADOR")
                .anyRequest().denyAll())
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter(acceso))));
        return http.build();
    }

    @Bean
    JwtDecoder jwtDecoder(JwtService jwtService) {
        return jwtService.decoder();
    }

    /**
     * El token solo identifica al usuario: el estado real (activo, rol, cambio de contraseña pendiente) se lee de la BD
     * (con una cache de segundos). Un usuario dado de baja pierde el acceso casi de inmediato, y quien debe cambiar su
     * contraseña solo puede usar /api/auth/me y /api/auth/password.
     */
    private Converter<Jwt, AbstractAuthenticationToken> converter(UsuarioAcceso acceso) {
        return jwt -> {
            var u = acceso.activo(jwt.getSubject())
                    .orElseThrow(() -> new BadCredentialsException("Usuario inexistente o inactivo"));
            List<GrantedAuthority> permisos = u.isDebeCambiarPassword()
                    ? List.of(new SimpleGrantedAuthority("CAMBIO_PASSWORD_PENDIENTE"))
                    : List.of(new SimpleGrantedAuthority("ROLE_" + u.getRol().name()));
            return new JwtAuthenticationToken(jwt, permisos, u.getEmail());
        };
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.origins:http://localhost:5173}") String origins) {
        CorsConfiguration cfg = new CorsConfiguration();
        List<String> lista = Arrays.stream(origins.split(",")).map(String::trim).toList();
        cfg.setAllowedOrigins(lista);
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cfg);
        return source;
    }
}
