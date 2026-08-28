package com.logitrack.service;

import com.logitrack.dto.ProductoRiesgoDTO;
import com.logitrack.model.EstadoOrdenCompra;
import com.logitrack.model.MovimientoInventario;
import com.logitrack.model.Producto;
import com.logitrack.model.Proveedor;
import com.logitrack.model.TipoMovimiento;
import com.logitrack.repository.BodegaRepository;
import com.logitrack.repository.InventarioBodegaRepository;
import com.logitrack.repository.MovimientoDetalleRepository;
import com.logitrack.repository.MovimientoInventarioRepository;
import com.logitrack.repository.OrdenCompraRepository;
import com.logitrack.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Cubre las reglas de "productos en riesgo" definidas en docs/sdd/02-especificacion.md
 * (sección 3):
 *
 *  - Caso 1: si el consumo en los últimos 30 días es 0, consumoDiarioPromedio y
 *    diasCobertura deben ser null, el estado debe ser "SIN_CONSUMO", y el producto
 *    NO debe aparecer en la lista de productos en riesgo.
 *  - Caso 2: si el stock total es EXACTAMENTE igual al punto de reorden, el producto
 *    NO está en riesgo (la condición del enunciado es estrictamente menor).
 *
 * ESTADO: escrito en Día 2 (rojo).
 * Este archivo NO COMPILA todavía porque hacen falta, como mínimo:
 *   - Producto.proveedorPrincipal (campo nuevo)
 *   - com.logitrack.dto.ProductoRiesgoDTO
 *   - com.logitrack.repository.MovimientoDetalleRepository (+ sumSalidasPorProductoEnRango)
 *   - com.logitrack.service.InventarioAnalyticsService / InventarioAnalyticsServiceImpl
 * Eso es exactamente la evidencia "en rojo" que se pide antes de implementar.
 */
@ExtendWith(MockitoExtension.class)
class InventarioAnalyticsServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private InventarioBodegaRepository inventarioBodegaRepository;

    @Mock
    private MovimientoDetalleRepository movimientoDetalleRepository;

    @Mock
    private MovimientoInventarioRepository movimientoInventarioRepository;

    @Mock
    private OrdenCompraRepository ordenCompraRepository;

    @Mock
    private BodegaRepository bodegaRepository;

    @InjectMocks
    private InventarioAnalyticsServiceImpl inventarioAnalyticsService;

    private Producto productoConProveedor;

    @BeforeEach
    void setUp() {
        Proveedor proveedor = Proveedor.builder()
                .id(1L)
                .nombre("Proveedor Test")
                .diasEntrega(10)
                .build();

        productoConProveedor = Producto.builder()
                .id(100L)
                .nombre("Producto en prueba")
                .categoria("Test")
                .stock(0) // irrelevante: la fuente de verdad es InventarioBodegaRepository
                .precio(BigDecimal.TEN)
                .proveedorPrincipal(proveedor)
                .build();
    }

    @Test
    void productoSinConsumoEn30Dias_quedaSinConsumo_yNoEntraEnRiesgo() {
        when(productoRepository.findAll()).thenReturn(List.of(productoConProveedor));
        when(inventarioBodegaRepository.sumStockByProductoId(100L)).thenReturn(5);
        when(movimientoDetalleRepository.sumSalidasPorProductoEnRango(
                eq(100L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0);

        List<ProductoRiesgoDTO> enRiesgo = inventarioAnalyticsService.obtenerProductosEnRiesgo();

        assertThat(enRiesgo)
                .as("Un producto sin consumo en los últimos 30 días no puede calificar como en riesgo")
                .noneMatch(p -> p.getProductoId().equals(100L));
    }

    @Test
    void stockIgualAlPuntoDeReorden_noEstaEnRiesgo() {
        // consumoDiarioPromedio = 30 salidas / 30 dias = 1.0
        // puntoReorden = 1.0 * diasEntrega(10) * 1.5 = 15.0
        when(productoRepository.findAll()).thenReturn(List.of(productoConProveedor));
        when(inventarioBodegaRepository.sumStockByProductoId(100L)).thenReturn(15); // == puntoReorden
        when(movimientoDetalleRepository.sumSalidasPorProductoEnRango(
                eq(100L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(30);

        List<ProductoRiesgoDTO> enRiesgo = inventarioAnalyticsService.obtenerProductosEnRiesgo();

        assertThat(enRiesgo)
                .as("stock == puntoReorden no debe considerarse en riesgo (regla estrictamente menor)")
                .noneMatch(p -> p.getProductoId().equals(100L));
    }
}
