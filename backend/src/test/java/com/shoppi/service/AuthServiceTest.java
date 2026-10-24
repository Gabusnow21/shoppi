package com.shoppi.service;

import com.shoppi.dto.auth.AuthResponse;
import com.shoppi.dto.auth.LoginRequest;
import com.shoppi.dto.auth.RegisterRequest;
import com.shoppi.entity.Usuario;
import com.shoppi.exception.CredencialesInvalidasException;
import com.shoppi.repository.UsuarioRepository;
import com.shoppi.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de {@link AuthService} con repositorio simulado.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String JWT_SECRET =
            "test-secret-shoppi-automatizado-0123456789abcdef0123456789abcdef";

    @Mock
    private UsuarioRepository usuarios;

    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        jwtService = new JwtService(JWT_SECRET, 60);
        authService = new AuthService(usuarios, passwordEncoder, jwtService);
    }

    @Test
    void registrarNormalizaCorreoGuardaHashYTelefonoVacioComoNull() {
        when(usuarios.existsByEmail("juan@ejemplo.com")).thenReturn(false);
        when(usuarios.saveAndFlush(any(Usuario.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        AuthResponse respuesta = authService.registrar(new RegisterRequest(
                "  Juan Pérez  ",
                "  Juan@Ejemplo.COM  ",
                "contraseña123",
                "   "));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).saveAndFlush(captor.capture());
        Usuario guardado = captor.getValue();

        assertEquals("Juan Pérez", guardado.getNombre());
        assertEquals("juan@ejemplo.com", guardado.getEmail());
        assertNull(guardado.getTelefono());
        assertNotEquals("contraseña123", guardado.getPasswordHash());
        assertTrue(passwordEncoder.matches("contraseña123", guardado.getPasswordHash()));

        assertEquals("juan@ejemplo.com", respuesta.email());
        assertEquals("Juan Pérez", respuesta.nombre());
        assertTrue(jwtService.isValid(respuesta.token()));
        assertEquals("juan@ejemplo.com", jwtService.extractEmail(respuesta.token()));
    }

    @Test
    void registrarCorreoDuplicadoFallaSinGuardar() {
        when(usuarios.existsByEmail("juan@ejemplo.com")).thenReturn(true);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> authService.registrar(new RegisterRequest(
                        "Juan Pérez", "juan@ejemplo.com", "contrasena123", null)));

        assertEquals("El correo ya está registrado.", error.getMessage());
        verify(usuarios, never()).saveAndFlush(any(Usuario.class));
    }

    @Test
    void registrarCorreoDuplicadoPorCarreraFallaConMensajeClaro() {
        when(usuarios.existsByEmail("juan@ejemplo.com")).thenReturn(false);
        when(usuarios.saveAndFlush(any(Usuario.class)))
                .thenThrow(new DataIntegrityViolationException("unique"));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> authService.registrar(new RegisterRequest(
                        "Juan Pérez", "juan@ejemplo.com", "contrasena123", null)));

        assertEquals("El correo ya está registrado.", error.getMessage());
    }

    @Test
    void iniciarSesionDevuelveTokenParaCredencialesValidas() {
        Usuario usuario = new Usuario(
                "Juan Pérez", "juan@ejemplo.com", passwordEncoder.encode("contrasena123"), null);
        ReflectionTestUtils.setField(usuario, "id", 42L);
        when(usuarios.findByEmail("juan@ejemplo.com")).thenReturn(Optional.of(usuario));

        AuthResponse respuesta = authService.iniciarSesion(
                new LoginRequest("Juan@Ejemplo.com", "contrasena123"));

        assertEquals(42L, respuesta.id());
        assertEquals("juan@ejemplo.com", respuesta.email());
        assertTrue(jwtService.isValid(respuesta.token()));
        assertEquals(42L, jwtService.extractUsuarioId(respuesta.token()));
    }

    @Test
    void iniciarSesionConPasswordIncorrectaFallaConMensajeGenerico() {
        Usuario usuario = new Usuario(
                "Juan Pérez", "juan@ejemplo.com", passwordEncoder.encode("contrasena123"), null);
        when(usuarios.findByEmail("juan@ejemplo.com")).thenReturn(Optional.of(usuario));

        CredencialesInvalidasException error = assertThrows(
                CredencialesInvalidasException.class,
                () -> authService.iniciarSesion(new LoginRequest("juan@ejemplo.com", "otraClave123")));

        assertEquals("Correo o contraseña incorrectos.", error.getMessage());
    }

    @Test
    void iniciarSesionConCorreoDesconocidoFallaConMensajeGenerico() {
        when(usuarios.findByEmail("nadie@ejemplo.com")).thenReturn(Optional.empty());

        CredencialesInvalidasException error = assertThrows(
                CredencialesInvalidasException.class,
                () -> authService.iniciarSesion(new LoginRequest("nadie@ejemplo.com", "contrasena123")));

        assertEquals("Correo o contraseña incorrectos.", error.getMessage());
    }
}
