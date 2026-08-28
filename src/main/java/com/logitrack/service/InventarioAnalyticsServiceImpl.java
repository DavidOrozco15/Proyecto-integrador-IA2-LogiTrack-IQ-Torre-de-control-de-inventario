package com.logitrack.service;

import com.logitrack.dto.ProductoRiesgoDTO;
import com.logitrack.dto.StockPorBodegaDTO;
import com.logitrack.repository.BodegaRepository;
import com.logitrack.repository.BodegaRepository;
import com.logitrack.repository.InventarioBodegaRepository;
import com.logitrack.repository.MovimientoDetalleRepository;
import com.logitrack.repository.MovimientoInventarioRepository;
import com.logitrack.repository.OrdenCompraRepository;
import com.logitrack.repository.ProductoRepository;
import com.logitrack.model.EstadoOrdenCompra;
import com.logitrack.util.FechasBogota;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InventarioAnalyticsServiceImpl implements InventarioAnalyticsService {

    private final ProductoRepository productoRepository;
    private final InventarioBodegaRepository inventarioBodegaRepository;
    private final MovimientoDetalleRepository movimientoDetalleRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final OrdenCompraRepository ordenCompraRepository;

    public InventarioAnalyticsServiceImpl(ProductoRepository productoRepository,
                                          InventarioBodegaRepository inventarioBodegaRepository,
                                          MovimientoDetalleRepository movimientoDetalleRepository,
                                          MovimientoInventarioRepository movimientoInventarioRepository,
                                          OrdenCompraRepository ordenCompraRepository) {
        this.productoRepository = productoRepository;
        this.inventarioBodegaRepository = inventarioBodegaRepository;
        this.movimientoDetalleRepository = movimientoDetalleRepository;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
        this.ordenCompraRepository = ordenCompraRepository;
    }

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

    @Override
    public List<ProductoRiesgoDTO> obtenerProductosEnQuiebre() {
        List<ProductoRiesgoDTO> enQuiebre = new ArrayList<>();
        var productos = productoRepository.findAll();

        for (var producto : productos) {
            Integer stockTotal = inventarioBodegaRepository.sumStockByProductoId(producto.getId());
            if (stockTotal == null) stockTotal = 0;

            if (stockTotal == 0) {
                enQuiebre.add(ProductoRiesgoDTO.builder()
                        .productoId(producto.getId())
                        .nombre(producto.getNombre())
                        .categoria(producto.getCategoria())
                        .stockTotal(0)
                        .build());
            }
        }
        return enQuiebre;
    }

    @Override
    public List<StockPorBodegaDTO> obtenerOcupacionPorBodega() {
        return bodegaRepository.findAll().stream()
                .map(bodega -> {
                    Integer stockTotal = inventarioBodegaRepository.sumStockByBodegaId(bodega.getId());
                    long stock = stockTotal != null ? stockTotal.longValue() : 0L;
                    int capacidad = bodega.getCapacidad() != null ? bodega.getCapacidad() : 0;
                    double porcentaje = capacidad > 0 ? (stock * 100.0 / capacidad) : 0.0;
                    return StockPorBodegaDTO.builder()
                            .bodegaId(bodega.getId())
                            .bodegaNombre(bodega.getNombre())
                            .stockTotal(stock)
                            .capacidad(capacidad)
                            .porcentajeOcupacion(Math.round(porcentaje * 10.0) / 10.0)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> obtenerKPIsCompletos() {
        var productosRiesgo = obtenerProductosEnRiesgo();
        var productosQuiebre = obtenerProductosEnQuiebre();
        var ocupacionBodegas = obtenerOcupacionPorBodega();

        var ordenesBorrador = ordenCompraRepository.findByEstado(EstadoOrdenCompra.BORRADOR);
        long cantidadOrdenes = ordenesBorrador.size();
        BigDecimal montoTotal = ordenCompraRepository.sumTotalByEstado(EstadoOrdenCompra.BORRADOR);
        if (montoTotal == null) montoTotal = BigDecimal.ZERO;

        var hoy = LocalDateTime.now(ZoneId.of("America/Bogota"));
        var ayer = hoy.toLocalDate().minusDays(1);
        var movimientos = movimientoInventarioRepository.buscarPorRangoFechas(
                ayer.atStartOfDay(), ayer.atTime(23, 59, 59));

        long entradas = movimientos.stream().filter(m -> m.getTipoMovimiento().name().equals("ENTRADA")).count();
        long salidas = movimientos.stream().filter(m -> m.getTipoMovimiento().name().equals("SALIDA")).count();
        long transferencias = movimientos.stream().filter(m -> m.getTipoMovimiento().name().equals("TRANSFERENCIA")).count();

        ZonedDateTime calculadoEn = ZonedDateTime.now(ZoneId.of("America/Bogota"));

        Map<String, Object> result = new HashMap<>();
        result.put("calculadoEn", calculadoEn.toString());
        result.put("ocupacionPorBodega", ocupacionBodegas.stream()
                .map(o -> Map.of(
                        "bodegaId", o.getBodegaId(),
                        "nombre", o.getBodegaNombre(),
                        "porcentaje", o.getPorcentajeOcupacion()))
                .collect(Collectors.toList()));
        result.put("productosEnQuiebre", productosQuiebre.size());
        result.put("productosEnRiesgo", productosRiesgo.size());
        result.put("ordenesPorAprobar", Map.of(
                "cantidad", cantidadOrdenes,
                "montoTotal", montoTotal.doubleValue()));
        result.put("movimientosAyer", Map.of(
                "entrada", entradas,
                "salida", salidas,
                "transferencia", transferencias));

        return result;
    }
}