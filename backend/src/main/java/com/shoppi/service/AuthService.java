package com.shoppi.service;

import com.shoppi.dto.auth.AuthResponse;
import com.shoppi.dto.auth.LoginRequest;
import com.shoppi.dto.auth.RegisterRequest;
import com.shoppi.entity.Usuario;
import com.shoppi.exception.CredencialesInvalidasException;
import com.shoppi.repository.UsuarioRepository;
import com.shoppi.security.JwtService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Registro e inicio de sesión con JWT propio.
 *
 * <p>Las contraseñas se almacenan con BCrypt. El correo se normaliza a
 * minúsculas para que {@code Ana@correo.com} y {@code ana@correo.com} sean el
 * mismo usuario. Las credenciales inválidas se reportan con
 * {@link CredencialesInvalidasException} (HTTP 401) sin distinguir entre correo
 * inexistente y contraseña incorrecta.</p>
 */
@Service
public class AuthService {

    private static final String CREDENCIALES_INVALIDAS = "Correo o contraseña incorrectos.";

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UsuarioRepository usuarios,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** Registra un usuario nuevo y devuelve su JWT. */
    @Transactional
    public AuthResponse registrar(RegisterRequest request) {
        String email = normalizarEmail(request.email());

        if (usuarios.existsByEmail(email)) {
            throw new IllegalArgumentException("El correo ya está registrado.");
        }

        Usuario usuario = new Usuario(
                request.nombre().trim(),
                email,
                passwordEncoder.encode(request.password()),
                normalizarTelefono(request.telefono()));

        try {
            // saveAndFlush fuerza la restricción UNIQUE dentro del try y evita
            // una carrera entre existsByEmail y el INSERT.
            usuarios.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("El correo ya está registrado.");
        }

        return respuesta(usuario);
    }

    /** Valida las credenciales y devuelve un JWT nuevo. */
    @Transactional(readOnly = true)
    public AuthResponse iniciarSesion(LoginRequest request) {
        Usuario usuario = usuarios.findByEmail(normalizarEmail(request.email()))
                .orElseThrow(() -> new CredencialesInvalidasException(CREDENCIALES_INVALIDAS));

        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException(CREDENCIALES_INVALIDAS);
        }

        return respuesta(usuario);
    }

    private AuthResponse respuesta(Usuario usuario) {
        String token = jwtService.generateToken(usuario.getId(), usuario.getEmail());
        return new AuthResponse(usuario.getId(), usuario.getNombre(), usuario.getEmail(), token);
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizarTelefono(String telefono) {
        if (telefono == null || telefono.isBlank()) {
            return null;
        }
        return telefono.trim();
    }
}
