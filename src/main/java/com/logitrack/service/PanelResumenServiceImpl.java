package com.logitrack.service;

import com.logitrack.dto.ResumenPanelRequest;
import com.logitrack.exception.BadRequestException;
import com.logitrack.model.ResumenPanel;
import com.logitrack.repository.AuditoriaRepository;
import com.logitrack.repository.BodegaRepository;
import com.logitrack.repository.OrdenCompraRepository;
import com.logitrack.repository.ProductoRepository;
import com.logitrack.repository.ProveedorRepository;
import com.logitrack.repository.ResumenPanelRepository;
import com.logitrack.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PanelResumenServiceImpl implements PanelResumenService {

    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;
    private final BodegaRepository bodegaRepository;
    private final OrdenCompraRepository ordenCompraRepository;
    private final ResumenPanelRepository resumenPanelRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaRepository auditoriaRepository;

    @Override
    @Transactional
    public void publicar(ResumenPanelRequest request) throws BadRequestException {
        if (request.getAlertas() != null) {
            for (var alerta : request.getAlertas()) {
                if (alerta.getProductoId() != null && !productoRepository.existsById(alerta.getProductoId())) {
                    throw new BadRequestException("Producto no encontrado: " + alerta.getProductoId());
                }
                if (alerta.getOrdenId() != null && !ordenCompraRepository.existsById(alerta.getOrdenId())) {
                    throw new BadRequestException("Orden no encontrada: " + alerta.getOrdenId());
                }
                if (alerta.getBodegaId() != null && !bodegaRepository.existsById(alerta.getBodegaId())) {
                    throw new BadRequestException("Bodega no encontrada: " + alerta.getBodegaId());
                }
            }
        }
        if (request.getAccionesSugeridas() != null) {
            for (var accion : request.getAccionesSugeridas()) {
                if (accion.getProductoId() != null && !productoRepository.existsById(accion.getProductoId())) {
                    throw new BadRequestException("Producto no encontrado: " + accion.getProductoId());
                }
                if (accion.getOrdenId() != null && !ordenCompraRepository.existsById(accion.getOrdenId())) {
                    throw new BadRequestException("Orden no encontrada: " + accion.getOrdenId());
                }
                if (accion.getBodegaId() != null && !bodegaRepository.existsById(accion.getBodegaId())) {
                    throw new BadRequestException("Bodega no encontrada: " + accion.getBodegaId());
                }
            }
        }

        ResumenPanel resumen = ResumenPanel.builder()
                .fecha(request.getFecha())
                .narrativa(request.getNarrativa())
                .build();

        resumenPanelRepository.save(resumen);
    }
}