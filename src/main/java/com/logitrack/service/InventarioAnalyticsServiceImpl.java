package com.logitrack.service;

import com.logitrack.dto.ProductoRiesgoDTO;
import com.logitrack.repository.InventarioBodegaRepository;
import com.logitrack.repository.MovimientoDetalleRepository;
import com.logitrack.repository.ProductoRepository;
import com.logitrack.util.FechasBogota;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventarioAnalyticsServiceImpl implements InventarioAnalyticsService {

    private final ProductoRepository productoRepository;
    private final InventarioBodegaRepository inventarioBodegaRepository;
    private final MovimientoDetalleRepository movimientoDetalleRepository;

    @Override
    public List<ProductoRiesgoDTO> obtenerProductosEnRiesgo() {
        List<ProductoRiesgoDTO> enRiesgo = new ArrayList<>();
        var productos = productoRepository.findAll();
        var inicio = FechasBogota.inicioUltimos30DiasBogota();
        var fin = FechasBogota.finDeHoyBogota();

        for (var producto : productos) {
            Integer stockTotal = inventarioBodegaRepository.sumStockByProductoId(producto.getId());
            if (stockTotal == null) stockTotal = 0;

            Integer salidas30 = movimientoDetalleRepository.sumSalidasPorProductoEnRango(
                    producto.getId(), inicio, fin);
            if (salidas30 == null) salidas30 = 0;

            if (salidas30 == 0) continue;

            double consumoDiario = salidas30 / 30.0;
            int diasEntrega = (producto.getProveedorPrincipal() != null)
                    ? producto.getProveedorPrincipal().getDiasEntrega() : 7;
            double puntoReorden = consumoDiario * diasEntrega * 1.5;
            double diasCobertura = stockTotal / consumoDiario;

            if (stockTotal < puntoReorden) {
                enRiesgo.add(ProductoRiesgoDTO.builder()
                        .productoId(producto.getId())
                        .nombre(producto.getNombre())
                        .categoria(producto.getCategoria())
                        .stockTotal(stockTotal)
                        .puntoReorden((int) Math.ceil(puntoReorden))
                        .consumoDiarioPromedio(consumoDiario)
                        .diasCobertura(diasCobertura)
                        .estado("EN_RIESGO")
                        .proveedor(producto.getProveedorPrincipal() != null
                                ? producto.getProveedorPrincipal().getNombre() : null)
                        .build());
            }
        }
        return enRiesgo;
    }
}