package com.shoppi.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración del filtro JWT contra un endpoint protegido de prueba.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(JwtSecurityIntegrationTest.ControladorDePrueba.class)
class JwtSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Value("${shoppi.jwt.secret}")
    private String secretoJwt;

    @Test
    void tokenValidoAccedeAlEndpointProtegido() throws Exception {
        String token = jwtService.generateToken(42L, "juan@ejemplo.com");

        mockMvc.perform(get("/api/test/protegido")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.email").value("juan@ejemplo.com"));
    }

    @Test
    void tokenValidoConEsquemaEnMinusculasTambienAccede() throws Exception {
        String token = jwtService.generateToken(7L, "ana@ejemplo.com");

        mockMvc.perform(get("/api/test/protegido")
                        .header(HttpHeaders.AUTHORIZATION, "bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.email").value("ana@ejemplo.com"));
    }

    @Test
    void sinTokenDevuelve401ConFormatoEstandar() throws Exception {
        mockMvc.perform(get("/api/test/protegido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Autenticación requerida."))
                .andExpect(jsonPath("$.path").value("/api/test/protegido"));
    }

    @Test
    void tokenInvalidoDevuelve401() throws Exception {
        mockMvc.perform(get("/api/test/protegido")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer esto-no-es-un-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autenticación requerida."));
    }

    @Test
    void tokenExpiradoDevuelve401() throws Exception {
        String tokenExpirado = new JwtService(secretoJwt, -1)
                .generateToken(42L, "juan@ejemplo.com");

        mockMvc.perform(get("/api/test/protegido")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenExpirado))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Autenticación requerida."));
    }

    @Test
    void encabezadoSinEsquemaBearerDevuelve401() throws Exception {
        String token = jwtService.generateToken(42L, "juan@ejemplo.com");

        mockMvc.perform(get("/api/test/protegido")
                        .header(HttpHeaders.AUTHORIZATION, "Basic " + token))
                .andExpect(status().isUnauthorized());
    }

    /** Endpoint solo para verificar que la autenticación llega al controlador. */
    @RestController
    @RequestMapping("/api/test")
    static class ControladorDePrueba {

        @GetMapping("/protegido")
        Map<String, Object> protegido(@AuthenticationPrincipal UsuarioAutenticado usuario) {
            return Map.of("id", usuario.id(), "email", usuario.email());
        }
    }
}
