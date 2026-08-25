package com.logitrack.service;

import com.logitrack.dto.ResumenPanelRequest;
import com.logitrack.exception.BadRequestException;
import com.logitrack.repository.ProductoRepository;
import com.logitrack.repository.OrdenCompraRepository;
import com.logitrack.repository.ResumenPanelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PanelResumenServiceImpl implements PanelResumenService {

    private final ProductoRepository productoRepository;
    private final OrdenCompraRepository ordenCompraRepository;
    private final ResumenPanelRepository resumenPanelRepository;

    @Override
    @Transactional
    public void publicar(ResumenPanelRequest request) throws BadRequestException {
        if (request.getAlertas() != null) {
            for (var alerta : request.getAlertas()) {
                if (alerta.getProductoId() != null && !productoRepository.existsById(alerta.getProductoId())) {
                    throw new BadRequestException("Producto no encontrado: " + alerta.getProductoId());
                }
            }
        }
        if (request.getAccionesSugeridas() != null) {
            for (var accion : request.getAccionesSugeridas()) {
                if (accion.getOrdenId() != null && !ordenCompraRepository.existsById(accion.getOrdenId())) {
                    throw new BadRequestException("Orden no encontrada: " + accion.getOrdenId());
                }
            }
        }
    }
}