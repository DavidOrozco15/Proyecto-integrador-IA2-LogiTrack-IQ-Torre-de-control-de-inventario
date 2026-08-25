package com.logitrack.service;

import com.logitrack.config.UserContext;
import com.logitrack.exception.BadRequestException;
import com.logitrack.model.Bodega;
import com.logitrack.model.EstadoOrdenCompra;
import com.logitrack.model.MovimientoInventario;
import com.logitrack.model.OrdenCompra;
import com.logitrack.model.Producto;
import com.logitrack.model.Proveedor;
import com.logitrack.model.TipoMovimiento;
import com.logitrack.repository.AuditoriaRepository;
import com.logitrack.repository.BodegaRepository;
import com.logitrack.repository.OrdenCompraRepository;
import com.logitrack.repository.ProductoRepository;
import com.logitrack.repository.ProveedorRepository;
import com.logitrack.repository.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cubre las reglas de creación y transición de estados de OrdenCompra
 * (docs/sdd/02-especificacion.md, secciones 4 y 11 — casos 3, 4 y 5).
 *
 * ESTADO: escrito en Día 2 (rojo).
 * No compila todavía porque hacen falta, como mínimo:
 *   - com.logitrack.model.OrdenCompra, EstadoOrdenCompra, Proveedor
 *   - com.logitrack.repository.OrdenCompraRepository, ProveedorRepository
 *   - com.logitrack.service.OrdenCompraService / OrdenCompraServiceImpl
 */
@ExtendWith(MockitoExtension.class)
class OrdenCompraServiceTest {

    @Mock private OrdenCompraRepository ordenCompraRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private ProveedorRepository proveedorRepository;
    @Mock private BodegaRepository bodegaRepository;
    @Mock private MovimientoInventarioService movimientoInventarioService;
    @Mock private AuditoriaRepository auditoriaRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks
    private OrdenCompraServiceImpl ordenCompraService;

    @BeforeEach
    void setUp() {
        // OrdenCompraServiceImpl.crear()/cambiarEstado() leen el usuario autenticado
        // desde este ThreadLocal (igual que el resto de los ServiceImpl del proyecto).
        UserContext.setUsername("admin");
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    // --- Caso 3: cantidad 0 o negativa al crear una orden -> 400 ---------------

    @Test
    void crearOrden_conCantidadCero_lanzaBadRequest() {
        OrdenCompra orden = ordenBase(0);

        assertThatThrownBy(() -> ordenCompraService.crear(orden))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void crearOrden_conCantidadNegativa_lanzaBadRequest() {
        OrdenCompra orden = ordenBase(-5);

        assertThatThrownBy(() -> ordenCompraService.crear(orden))
                .isInstanceOf(BadRequestException.class);
    }

    // --- Caso 4: una orden CANCELADA no se puede aprobar -----------------------

    @Test
    void cambiarEstado_deCanceladaAAprobada_lanzaBadRequest() {
        OrdenCompra ordenCancelada = ordenBase(10);
        ordenCancelada.setId(1L);
        ordenCancelada.setEstado(EstadoOrdenCompra.CANCELADA);

        when(ordenCompraRepository.findById(1L)).thenReturn(Optional.of(ordenCancelada));

        assertThatThrownBy(() -> ordenCompraService.cambiarEstado(1L, EstadoOrdenCompra.APROBADA))
                .isInstanceOf(BadRequestException.class);

        verify(movimientoInventarioService, never()).registrarMovimiento(any());
    }

    // --- Caso 5: APROBADA -> RECIBIDA genera un movimiento ENTRADA -------------

    @Test
    void cambiarEstado_deAprobadaARecibida_generaMovimientoEntradaConDatosCorrectos() {
        Bodega bodegaDestino = Bodega.builder()
                .id(9L).nombre("Bodega Destino").ubicacion("X").capacidad(1000).build();
        Producto producto = Producto.builder()
                .id(50L).nombre("Producto X").stock(0).precio(BigDecimal.TEN).build();

        OrdenCompra ordenAprobada = OrdenCompra.builder()
                .id(2L)
                .producto(producto)
                .bodegaDestino(bodegaDestino)
                .cantidad(30)
                .precioUnitario(BigDecimal.valueOf(5))
                .total(BigDecimal.valueOf(150))
                .estado(EstadoOrdenCompra.APROBADA)
                .build();

        when(ordenCompraRepository.findById(2L)).thenReturn(Optional.of(ordenAprobada));
        when(ordenCompraRepository.save(any(OrdenCompra.class))).thenAnswer(inv -> inv.getArgument(0));
        when(movimientoInventarioService.registrarMovimiento(any(MovimientoInventario.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ordenCompraService.cambiarEstado(2L, EstadoOrdenCompra.RECIBIDA);

        ArgumentCaptor<MovimientoInventario> captor = ArgumentCaptor.forClass(MovimientoInventario.class);
        verify(movimientoInventarioService).registrarMovimiento(captor.capture());

        MovimientoInventario movimientoCreado = captor.getValue();
        assertThat(movimientoCreado.getTipoMovimiento()).isEqualTo(TipoMovimiento.ENTRADA);
        assertThat(movimientoCreado.getBodegaDestino().getId()).isEqualTo(9L);
        assertThat(movimientoCreado.getDetalles()).hasSize(1);
        assertThat(movimientoCreado.getDetalles().get(0).getProducto().getId()).isEqualTo(50L);
        assertThat(movimientoCreado.getDetalles().get(0).getCantidad()).isEqualTo(30);
    }

    private OrdenCompra ordenBase(int cantidad) {
        Producto producto = Producto.builder().id(1L).nombre("Producto").stock(0).precio(BigDecimal.ONE).build();
        Bodega bodega = Bodega.builder().id(1L).nombre("Bodega").ubicacion("X").capacidad(100).build();
        Proveedor proveedor = Proveedor.builder().id(1L).nombre("Proveedor").diasEntrega(5).build();

        return OrdenCompra.builder()
                .producto(producto)
                .proveedor(proveedor)
                .bodegaDestino(bodega)
                .cantidad(cantidad)
                .precioUnitario(BigDecimal.TEN)
                .build();
    }
}
