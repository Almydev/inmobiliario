package co.inmobiliaria360.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.inmobiliaria360.domain.Rol;
import co.inmobiliaria360.domain.Usuario;
import co.inmobiliaria360.repository.ComprobanteEgresoRepository;
import co.inmobiliaria360.repository.CuentaCobroRepository;
import co.inmobiliaria360.repository.UsuarioRepository;
import co.inmobiliaria360.service.ComprobanteEgresoService;
import co.inmobiliaria360.service.CuentaCobroService;
import co.inmobiliaria360.web.ApiExceptionHandler;
import co.inmobiliaria360.web.AuthController;
import co.inmobiliaria360.web.ComprobanteEgresoController;
import co.inmobiliaria360.web.CuentaCobroController;
import co.inmobiliaria360.web.UsuarioController;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import java.util.Base64;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Pruebas de seguridad de la API: autenticacion, autorizacion, tokens manipulados, validacion de entradas, CORS y fuerza bruta. */
@WebMvcTest(controllers = {AuthController.class, UsuarioController.class, CuentaCobroController.class, ComprobanteEgresoController.class})
@Import({SecurityConfig.class, JwtService.class, LoginThrottle.class, ApiExceptionHandler.class})
@TestPropertySource(properties = {
        "app.jwt.secret=0123456789abcdef0123456789abcdef",
        "app.cors.origins=https://app.prueba.test"
})
class SeguridadApiTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    @Autowired MockMvc mvc;
    @Autowired JwtService jwt;

    @MockitoBean UsuarioRepository usuarios;
    @MockitoBean CuentaCobroService cuentaService;
    @MockitoBean CuentaCobroRepository cuentaRepo;
    @MockitoBean ComprobanteEgresoService egresoService;
    @MockitoBean ComprobanteEgresoRepository egresoRepo;

    private String token(Rol rol) {
        var u = new Usuario();
        u.setEmail(rol.name().toLowerCase() + "@prueba.test");
        u.setNombre("Prueba");
        u.setRol(rol);
        return jwt.generar(u);
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    // ---------- autenticacion ----------

    @Test
    void sinTokenTodoLoDeApiEstaProtegido() throws Exception {
        mvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/cuentas-cobro")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/cuentas-cobro/1/enviar")).andExpect(status().isUnauthorized());
    }

    @Test
    void comprobantesDeEgresoRequierenTokenYValidanEntradas() throws Exception {
        mvc.perform(get("/api/comprobantes-egreso")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/comprobantes-egreso/1/pagar")).andExpect(status().isUnauthorized());
        String auth = bearer(token(Rol.OPERADOR));
        mvc.perform(post("/api/comprobantes-egreso").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inmuebleId\":1,\"periodo\":\"2026-10\",\"dias\":99}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/comprobantes-egreso").header("Authorization", auth).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inmuebleId\":1,\"periodo\":\"2026-10\",\"otrosDescuentos\":-5}"))
                .andExpect(status().isBadRequest());
        verify(egresoService, never()).generar(any(), any(), org.mockito.ArgumentMatchers.anyInt(), any(), any(), any());
    }

    @Test
    void tokenManipuladoSeRechaza() throws Exception {
        String valido = token(Rol.OPERADOR);
        String[] partes = valido.split("\\.");
        // Un operador cambia su rol a ADMIN en el payload sin volver a firmar
        String payload = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8).replace("OPERADOR", "ADMIN");
        String falso = partes[0] + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "." + partes[2];
        mvc.perform(get("/api/usuarios").header("Authorization", bearer(falso))).andExpect(status().isUnauthorized());
        char c = partes[2].charAt(0);
        String firmaAlterada = (c == 'A' ? 'B' : 'A') + partes[2].substring(1);
        mvc.perform(get("/api/usuarios").header("Authorization", bearer(partes[0] + "." + partes[1] + "." + firmaAlterada)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenFirmadoConOtraClaveSeRechaza() throws Exception {
        var otro = new JwtService("ffffffffffffffffffffffffffffffff", 8);
        var u = new Usuario();
        u.setEmail("admin@prueba.test");
        u.setNombre("X");
        u.setRol(Rol.ADMIN);
        mvc.perform(get("/api/usuarios").header("Authorization", bearer(otro.generar(u)))).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenExpiradoSeRechaza() throws Exception {
        var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
        var ahora = Instant.now();
        var claims = JwtClaimsSet.builder().subject("admin@prueba.test").claim("rol", "ADMIN")
                .issuedAt(ahora.minus(Duration.ofHours(3))).expiresAt(ahora.minus(Duration.ofHours(2))).build();
        String caducado = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        mvc.perform(get("/api/usuarios").header("Authorization", bearer(caducado))).andExpect(status().isUnauthorized());
    }

    @Test
    void tokenConAlgoritmoNoneSeRechaza() throws Exception {
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                "{\"sub\":\"admin@prueba.test\",\"rol\":\"ADMIN\",\"exp\":4102444800}".getBytes(StandardCharsets.UTF_8));
        mvc.perform(get("/api/usuarios").header("Authorization", bearer(header + "." + payload + ".")))
                .andExpect(status().isUnauthorized());
    }

    // ---------- autorizacion por rol ----------

    @Test
    void operadorNoPuedeGestionarUsuarios() throws Exception {
        mvc.perform(get("/api/usuarios").header("Authorization", bearer(token(Rol.OPERADOR)))).andExpect(status().isForbidden());
        mvc.perform(post("/api/usuarios").header("Authorization", bearer(token(Rol.OPERADOR)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.co\",\"nombre\":\"A\",\"rol\":\"ADMIN\",\"password\":\"12345678\"}"))
                .andExpect(status().isForbidden());
        verify(usuarios, never()).save(any());
    }

    @Test
    void adminSiPuedeListarUsuarios() throws Exception {
        mvc.perform(get("/api/usuarios").header("Authorization", bearer(token(Rol.ADMIN)))).andExpect(status().isOk());
    }

    @Test
    void operadorSiPuedeUsarLaApiOperativa() throws Exception {
        mvc.perform(get("/api/cuentas-cobro").header("Authorization", bearer(token(Rol.OPERADOR)))).andExpect(status().isOk());
    }

    @Test
    void rutasNoDefinidasQuedanDenegadas() throws Exception {
        mvc.perform(get("/actuator/env").header("Authorization", bearer(token(Rol.ADMIN)))).andExpect(status().is4xxClientError());
        mvc.perform(get("/otra-cosa")).andExpect(status().is4xxClientError());
    }

    // ---------- validacion de entradas ----------

    @Test
    void periodoInvalidoSeRechazaConBadRequest() throws Exception {
        for (String malo : new String[] {"2026-13", "2026-1", "abcd-ef", "2026-10'; DROP TABLE usuarios;--"}) {
            mvc.perform(post("/api/cuentas-cobro/generar-mes").header("Authorization", bearer(token(Rol.OPERADOR)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"periodo\":\"" + malo.replace("'", "\\u0027") + "\"}"))
                    .andExpect(status().isBadRequest());
        }
        verify(cuentaService, never()).generarMes(any());
    }

    @Test
    void elEnvioEnLoteTieneTopeDeDocumentos() throws Exception {
        String ids = LongStream.rangeClosed(1, 101).boxed().map(String::valueOf).collect(Collectors.joining(","));
        mvc.perform(post("/api/cuentas-cobro/enviar-lote").header("Authorization", bearer(token(Rol.OPERADOR)))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"ids\":[" + ids + "]}"))
                .andExpect(status().isBadRequest());
        verify(cuentaService, never()).enviar(any());
    }

    @Test
    void cuerpoMalformadoNoFiltraDetallesInternos() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{no es json"))
                .andExpect(status().is4xxClientError());
    }

    // ---------- CORS ----------

    @Test
    void corsPermiteSoloElOrigenConfigurado() throws Exception {
        mvc.perform(options("/api/auth/login").header("Origin", "https://app.prueba.test")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://app.prueba.test"));
        mvc.perform(options("/api/auth/login").header("Origin", "https://malicioso.test")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    // ---------- login y fuerza bruta ----------

    @Test
    void loginIncorrectoNoRevelaSiElUsuarioExiste() throws Exception {
        var u = new Usuario();
        u.setEmail("real@prueba.test");
        u.setRol(Rol.ADMIN);
        u.setNombre("Real");
        u.setPasswordHash(new BCryptPasswordEncoder().encode("correcta-123"));
        when(usuarios.findByEmail("real@prueba.test")).thenReturn(Optional.of(u));
        when(usuarios.findByEmail("noexiste@prueba.test")).thenReturn(Optional.empty());

        String r1 = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"real@prueba.test\",\"password\":\"mala\"}"))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String r2 = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"noexiste@prueba.test\",\"password\":\"mala\"}"))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertEquals(r1, r2);
    }

    @Test
    void tras5FallosElLoginSeBloqueaAunConLaClaveCorrecta() throws Exception {
        var u = new Usuario();
        u.setEmail("victima@prueba.test");
        u.setRol(Rol.OPERADOR);
        u.setNombre("V");
        u.setActivo(true);
        u.setPasswordHash(new BCryptPasswordEncoder().encode("correcta-123"));
        when(usuarios.findByEmail("victima@prueba.test")).thenReturn(Optional.of(u));

        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"victima@prueba.test\",\"password\":\"mala" + i + "\"}"))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"victima@prueba.test\",\"password\":\"correcta-123\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void usuarioInactivoNoPuedeIniciarSesion() throws Exception {
        var u = new Usuario();
        u.setEmail("baja@prueba.test");
        u.setRol(Rol.OPERADOR);
        u.setNombre("B");
        u.setActivo(false);
        u.setPasswordHash(new BCryptPasswordEncoder().encode("correcta-123"));
        when(usuarios.findByEmail("baja@prueba.test")).thenReturn(Optional.of(u));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"baja@prueba.test\",\"password\":\"correcta-123\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- cabeceras ----------

    @Test
    void respuestasLlevanCabecerasDeSeguridad() throws Exception {
        mvc.perform(get("/api/usuarios"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().exists("X-Frame-Options"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")));
    }
}
