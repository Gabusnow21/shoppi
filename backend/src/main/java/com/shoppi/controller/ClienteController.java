package com.shoppi.controller;

import com.shoppi.dto.cliente.ClienteRequest;
import com.shoppi.dto.cliente.ClienteResponse;
import com.shoppi.security.UsuarioAutenticado;
import com.shoppi.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * CRUD de clientes del usuario autenticado.
 *
 * <p>Requiere {@code Authorization: Bearer <token>}; el id del usuario sale del
 * JWT y nunca del cuerpo de la petición.</p>
 */
@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public List<ClienteResponse> listar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return clienteService.listar(usuario.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteResponse crear(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody ClienteRequest request) {
        return clienteService.crear(usuario.id(), request);
    }

    @GetMapping("/{id}")
    public ClienteResponse obtener(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id) {
        return clienteService.obtener(usuario.id(), id);
    }

    @PutMapping("/{id}")
    public ClienteResponse actualizar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id,
            @Valid @RequestBody ClienteRequest request) {
        return clienteService.actualizar(usuario.id(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(
            @AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id) {
        clienteService.eliminar(usuario.id(), id);
    }
}
