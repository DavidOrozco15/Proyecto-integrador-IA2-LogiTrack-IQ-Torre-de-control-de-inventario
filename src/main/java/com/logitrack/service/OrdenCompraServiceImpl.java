package com.logitrack.service;

import com.logitrack.exception.BadRequestException;
import com.logitrack.model.EstadoOrdenCompra;
import com.logitrack.model.MovimientoInventario;
import com.logitrack.model.MovimientoDetalle;
import com.logitrack.model.OrdenCompra;
import com.logitrack.model.TipoMovimiento;
import com.logitrack.repository.OrdenCompraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class OrdenCompraServiceImpl implements OrdenCompraService {

    private final OrdenCompraRepository ordenCompraRepository;
    private final MovimientoInventarioService movimientoInventarioService;

    @Override
    @Transactional
    public OrdenCompra crear(OrdenCompra orden) throws BadRequestException {
        if (orden.getCantidad() <= 0) {
            throw new BadRequestException("La cantidad debe ser mayor a 0");
        }
        return ordenCompraRepository.save(orden);
    }

    @Override
    @Transactional
    public OrdenCompra cambiarEstado(Long id, EstadoOrdenCompra nuevoEstado) throws BadRequestException {
        OrdenCompra orden = ordenCompraRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Orden no encontrada"));

        if (orden.getEstado() == EstadoOrdenCompra.CANCELADA &&
            nuevoEstado == EstadoOrdenCompra.APROBADA) {
            throw new BadRequestException("No se puede aprobar una orden cancelada");
        }

        EstadoOrdenCompra anterior = orden.getEstado();
        orden.setEstado(nuevoEstado);
        orden = ordenCompraRepository.save(orden);

        if (anterior == EstadoOrdenCompra.APROBADA && nuevoEstado == EstadoOrdenCompra.RECIBIDA) {
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
}