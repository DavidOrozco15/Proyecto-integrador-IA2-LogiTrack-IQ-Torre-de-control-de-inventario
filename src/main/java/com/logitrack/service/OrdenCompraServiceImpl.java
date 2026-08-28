package com.logitrack.service;

import com.logitrack.config.UserContext;
import com.logitrack.exception.BadRequestException;
import com.logitrack.exception.ResourceNotFoundException;
import com.logitrack.model.EstadoOrdenCompra;
import com.logitrack.model.MovimientoDetalle;
import com.logitrack.model.MovimientoInventario;
import com.logitrack.model.OrdenCompra;
import com.logitrack.model.TipoMovimiento;
import com.logitrack.repository.AuditoriaRepository;
import com.logitrack.repository.BodegaRepository;
import com.logitrack.repository.OrdenCompraRepository;
import com.logitrack.repository.ProductoRepository;
import com.logitrack.repository.ProveedorRepository;
import com.logitrack.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrdenCompraServiceImpl implements OrdenCompraService {

    private final OrdenCompraRepository ordenCompraRepository;
    private final ProductoRepository productoRepository;
    private final ProveedorRepository proveedorRepository;
    private final BodegaRepository bodegaRepository;
    private final MovimientoInventarioService movimientoInventarioService;
    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional
    public OrdenCompra crear(OrdenCompra orden) throws BadRequestException {
        if (orden.getCantidad() <= 0) {
            throw new BadRequestException("La cantidad debe ser mayor a 0");
        }
        if (orden.getPrecioUnitario() == null || orden.getPrecioUnitario().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("El precio unitario debe ser mayor a 0");
        }
        // Calcular total en el servidor
        orden.setTotal(orden.getPrecioUnitario().multiply(java.math.BigDecimal.valueOf(orden.getCantidad())));
        String username = UserContext.getUsername();
        return ordenCompraRepository.save(orden);
    }

    @Override
    @Transactional
    public OrdenCompra cambiarEstado(Long id, EstadoOrdenCompra nuevoEstado) throws BadRequestException {
        OrdenCompra orden = ordenCompraRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Orden no encontrada"));

        EstadoOrdenCompra estadoAnterior = orden.getEstado();

        if (estadoAnterior == EstadoOrdenCompra.CANCELADA && nuevoEstado == EstadoOrdenCompra.APROBADA) {
            throw new BadRequestException("No se puede aprobar una orden cancelada");
        }

        orden.setEstado(nuevoEstado);
        orden.setPdfBytes(null);
        orden = ordenCompraRepository.save(orden);

        if (estadoAnterior == EstadoOrdenCompra.APROBADA && nuevoEstado == EstadoOrdenCompra.RECIBIDA) {
            MovimientoDetalle detalle = MovimientoDetalle.builder()
                    .producto(orden.getProducto())
                    .cantidad(orden.getCantidad())
                    .build();

            MovimientoInventario movimiento = MovimientoInventario.builder()
                    .tipoMovimiento(TipoMovimiento.ENTRADA)
                    .bodegaDestino(orden.getBodegaDestino())
                    .detalles(Collections.singletonList(detalle))
                    .build();
            detalle.setMovimiento(movimiento);

            movimientoInventarioService.registrarMovimiento(movimiento);
        }

        return orden;
    }

    @Override
    public List<OrdenCompra> obtenerTodas() {
        return ordenCompraRepository.findAll();
    }

    @Override
    public List<OrdenCompra> obtenerPorEstado(EstadoOrdenCompra estado) {
        return ordenCompraRepository.findByEstado(estado);
    }

    @Override
    public OrdenCompra obtenerPorId(Long id) throws ResourceNotFoundException {
        return ordenCompraRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OrdenCompra", "id", id));
    }
}