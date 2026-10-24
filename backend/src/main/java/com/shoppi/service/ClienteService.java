package com.shoppi.service;

import com.shoppi.dto.cliente.ClienteRequest;
import com.shoppi.dto.cliente.ClienteResponse;
import com.shoppi.entity.Cliente;
import com.shoppi.entity.Usuario;
import com.shoppi.repository.ClienteRepository;
import com.shoppi.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CRUD de clientes con alcance por usuario.
 *
 * <p>Todas las operaciones reciben el id del usuario autenticado y filtran por
 * él: un usuario nunca puede leer, editar ni borrar clientes de otro. En esos
 * casos se responde 404 para no revelar su existencia.</p>
 */
@Service
public class ClienteService {

    private final ClienteRepository clientes;
    private final UsuarioRepository usuarios;

    public ClienteService(ClienteRepository clientes, UsuarioRepository usuarios) {
        this.clientes = clientes;
        this.usuarios = usuarios;
    }

    /** Lista los clientes del usuario autenticado. */
    @Transactional(readOnly = true)
    public List<ClienteResponse> listar(Long usuarioId) {
        return clientes.findByUsuarioId(usuarioId).stream()
                .map(this::respuesta)
                .toList();
    }

    /** Obtiene un cliente propio o lanza 404. */
    @Transactional(readOnly = true)
    public ClienteResponse obtener(Long usuarioId, Long clienteId) {
        return respuesta(buscarPropio(usuarioId, clienteId));
    }

    /** Crea un cliente para el usuario autenticado. */
    @Transactional
    public ClienteResponse crear(Long usuarioId, ClienteRequest request) {
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado."));

        Cliente cliente = new Cliente(
                usuario,
                request.nombre().trim(),
                limpiar(request.telefono()),
                limpiar(request.email()),
                limpiar(request.direccion()));

        return respuesta(clientes.save(cliente));
    }

    /** Actualiza un cliente propio o lanza 404. */
    @Transactional
    public ClienteResponse actualizar(Long usuarioId, Long clienteId, ClienteRequest request) {
        Cliente cliente = buscarPropio(usuarioId, clienteId);

        cliente.actualizar(
                request.nombre().trim(),
                limpiar(request.telefono()),
                limpiar(request.email()),
                limpiar(request.direccion()));

        return respuesta(cliente);
    }

    /** Elimina un cliente propio o lanza 404. */
    @Transactional
    public void eliminar(Long usuarioId, Long clienteId) {
        clientes.delete(buscarPropio(usuarioId, clienteId));
    }

    private Cliente buscarPropio(Long usuarioId, Long clienteId) {
        return clientes.findByIdAndUsuarioId(clienteId, usuarioId)
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado."));
    }

    private ClienteResponse respuesta(Cliente cliente) {
        return new ClienteResponse(
                cliente.getId(),
                cliente.getNombre(),
                cliente.getTelefono(),
                cliente.getEmail(),
                cliente.getDireccion());
    }

    private String limpiar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }
}
