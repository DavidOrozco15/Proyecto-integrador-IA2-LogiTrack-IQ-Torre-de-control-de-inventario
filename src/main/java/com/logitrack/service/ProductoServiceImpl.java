package com.logitrack.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.logitrack.config.UserContext;
import com.logitrack.dto.ProductoConInventarioDTO;
import com.logitrack.exception.BadRequestException;
import com.logitrack.exception.ResourceNotFoundException;
import com.logitrack.model.Auditoria;
import com.logitrack.model.Bodega;
import com.logitrack.model.InventarioBodega;
import com.logitrack.model.MovimientoDetalle;
import com.logitrack.model.MovimientoInventario;
import com.logitrack.model.Producto;
import com.logitrack.model.TipoOperacion;
import com.logitrack.model.Usuario;
import com.logitrack.repository.AuditoriaRepository;
import com.logitrack.repository.BodegaRepository;
import com.logitrack.repository.InventarioBodegaRepository;
import com.logitrack.repository.MovimientoInventarioRepository;
import com.logitrack.repository.ProductoRepository;
import com.logitrack.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductoServiceImpl implements ProductoService {

    private static final Logger log = LoggerFactory.getLogger(ProductoServiceImpl.class);

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final ProductoRepository productoRepository;
    private final InventarioBodegaRepository inventarioBodegaRepository;
    private final BodegaRepository bodegaRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final MovimientoInventarioRepository movimientoRepository;

    public ProductoServiceImpl(ProductoRepository productoRepository,
                                InventarioBodegaRepository inventarioBodegaRepository,
                                BodegaRepository bodegaRepository,
                                AuditoriaRepository auditoriaRepository,
                                UsuarioRepository usuarioRepository,
                                MovimientoInventarioRepository movimientoRepository) {
        this.productoRepository = productoRepository;
        this.inventarioBodegaRepository = inventarioBodegaRepository;
        this.bodegaRepository = bodegaRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.movimientoRepository = movimientoRepository;
    }

    @Override
    public List<Producto> obtenerTodos() {
        return productoRepository.findAll();
    }

    @Override
    public Producto obtenerPorId(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto", "id", id));
    }

    @Override
    @Transactional
    public Producto guardar(Producto producto) {
        producto.setStock(0);
        Producto saved = productoRepository.save(producto);
        guardarAuditoria(TipoOperacion.INSERT, saved, null, serializar(saved));
        return saved;
    }

    @Override
    @Transactional
    public Producto guardarConInventario(Producto producto, Map<Long, Integer> stockPorBodega) {
        int stockTotal = 0;
        producto.setStock(0);
        Producto saved = productoRepository.save(producto);

        if (stockPorBodega != null && !stockPorBodega.isEmpty()) {
            for (Map.Entry<Long, Integer> entry : stockPorBodega.entrySet()) {
                Long bodegaId = entry.getKey();
                Integer cantidad = entry.getValue();

                if (cantidad == null || cantidad <= 0) continue;

                Bodega bodega = bodegaRepository.findById(bodegaId)
                        .orElseThrow(() -> new ResourceNotFoundException("Bodega", "id", bodegaId));

                // Validar capacidad de la bodega
                Integer currentStock = inventarioBodegaRepository.sumStockByBodegaId(bodega.getId());
                if (bodega.getCapacidad() < currentStock + cantidad) {
                    throw new BadRequestException(String.format(
                            "La bodega '%s' tiene la capacidad al máximo. Capacidad: %d, Stock actual: %d, Intentando ingresar: %d",
                            bodega.getNombre(), bodega.getCapacidad(), currentStock, cantidad));
                }

                InventarioBodega inventario = InventarioBodega.builder()
                        .producto(saved)
                        .bodega(bodega)
                        .stock(cantidad)
                        .build();
                inventarioBodegaRepository.save(inventario);

                stockTotal += cantidad;
            }
        }

        saved.setStock(stockTotal);
        Producto updated = productoRepository.save(saved);
        guardarAuditoria(TipoOperacion.INSERT, updated, null, serializar(updated));
        return updated;
    }

    @Override
    @Transactional
    public Producto actualizar(Long id, Producto producto) {
        Producto productoExistente = obtenerPorId(id);
        String valoresAnteriores = serializar(productoExistente);

        productoExistente.setNombre(producto.getNombre());
        productoExistente.setCategoria(producto.getCategoria());
        productoExistente.setPrecio(producto.getPrecio());

        Producto saved = productoRepository.save(productoExistente);
        guardarAuditoria(TipoOperacion.UPDATE, saved, valoresAnteriores, serializar(saved));
        return saved;
    }

    @Override
    @Transactional
    public Producto actualizarConInventario(Long id, Producto producto, Map<Long, Integer> stockPorBodega) {
        Producto productoExistente = obtenerPorId(id);
        String valoresAnteriores = serializar(productoExistente);

        productoExistente.setNombre(producto.getNombre());
        productoExistente.setCategoria(producto.getCategoria());
        productoExistente.setPrecio(producto.getPrecio());

        // Limpiar inventarios existentes
        List<InventarioBodega> inventariosExistentes = inventarioBodegaRepository.findByProductoId(id);
        inventarioBodegaRepository.deleteAll(inventariosExistentes);

        // Re-crear inventarios
        int stockTotal = 0;
        if (stockPorBodega != null && !stockPorBodega.isEmpty()) {
            for (Map.Entry<Long, Integer> entry : stockPorBodega.entrySet()) {
                Long bodegaId = entry.getKey();
                Integer cantidad = entry.getValue();

                if (cantidad == null || cantidad <= 0) continue;

                Bodega bodega = bodegaRepository.findById(bodegaId)
                        .orElseThrow(() -> new ResourceNotFoundException("Bodega", "id", bodegaId));

                // Validar capacidad de la bodega
                Integer currentStock = inventarioBodegaRepository.sumStockByBodegaId(bodega.getId());
                if (bodega.getCapacidad() < currentStock + cantidad) {
                    throw new BadRequestException(String.format(
                            "La bodega '%s' tiene la capacidad al máximo. Capacidad: %d, Stock actual: %d, Intentando ingresar: %d",
                            bodega.getNombre(), bodega.getCapacidad(), currentStock, cantidad));
                }

                InventarioBodega inventario = InventarioBodega.builder()
                        .producto(productoExistente)
                        .bodega(bodega)
                        .stock(cantidad)
                        .build();
                inventarioBodegaRepository.save(inventario);

                stockTotal += cantidad;
            }
        }

        productoExistente.setStock(stockTotal);
        Producto saved = productoRepository.save(productoExistente);
        guardarAuditoria(TipoOperacion.UPDATE, saved, valoresAnteriores, serializar(saved));
        return saved;
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Producto producto = obtenerPorId(id);
        String valoresAnteriores = serializar(producto);

        // Eliminar inventario asociado a este producto en todas las bodegas
        List<InventarioBodega> inventarios = inventarioBodegaRepository.findByProductoId(id);
        if (!inventarios.isEmpty()) {
            inventarioBodegaRepository.deleteAll(inventarios);
        }

        // Eliminar los detalles de movimientos que referencian este producto
        // (los movimientos se mantienen, solo se eliminan los detalles)
        List<MovimientoInventario> todosMovimientos = movimientoRepository.findAllOrderByFechaDesc();
        for (MovimientoInventario mov : todosMovimientos) {
            boolean necesitaActualizar = false;
            var iter = mov.getDetalles().iterator();
            while (iter.hasNext()) {
                MovimientoDetalle detalle = iter.next();
                if (detalle.getProducto().getId().equals(id)) {
                    iter.remove();
                    necesitaActualizar = true;
                }
            }
            if (necesitaActualizar) {
                movimientoRepository.save(mov);
            }
        }

        productoRepository.delete(producto);
        guardarAuditoria(TipoOperacion.DELETE, producto, valoresAnteriores, null);
    }

    @Override
    public List<Producto> buscarPorNombre(String nombre) {
        return productoRepository.findByNombreContainingIgnoreCase(nombre);
    }

    @Override
    public List<Producto> buscarBajoStock(Integer umbral) {
        return productoRepository.findByStockLessThan(umbral != null ? umbral : 10);
    }

    @Override
    public List<Producto> filtrarProductos(String nombre, String categoria, Boolean bajoStock) {
        return productoRepository.filtrarProductos(
                (nombre != null && !nombre.isBlank()) ? nombre : null,
                (categoria != null && !categoria.isBlank()) ? categoria : null,
                bajoStock
        );
    }

    @Override
    public List<ProductoConInventarioDTO> obtenerTodosConInventario() {
        List<Producto> productos = productoRepository.findAll();
        List<Long> productoIds = productos.stream().map(Producto::getId).toList();
        List<InventarioBodega> todosInventarios = inventarioBodegaRepository.findByProductoIdIn(productoIds);

        Map<Long, List<InventarioBodega>> inventarioMap = todosInventarios.stream()
                .collect(Collectors.groupingBy(inv -> inv.getProducto().getId()));

        return productos.stream()
                .map(p -> ProductoConInventarioDTO.fromProducto(p,
                        inventarioMap.getOrDefault(p.getId(), List.of())))
                .toList();
    }

    @Override
    public ProductoConInventarioDTO obtenerConInventarioPorId(Long id) {
        Producto producto = obtenerPorId(id);
        List<InventarioBodega> inventarios = inventarioBodegaRepository.findByProductoId(id);
        return ProductoConInventarioDTO.fromProducto(producto, inventarios);
    }

    @Override
    public Map<String, Object> obtenerStockPorProducto(Long id) {
        Producto producto = obtenerPorId(id);
        List<InventarioBodega> inventarios = inventarioBodegaRepository.findByProductoId(id);
        
        int stockTotal = inventarios.stream()
                .mapToInt(InventarioBodega::getStock)
                .sum();
        
        Map<String, Object> result = Map.of(
                "productoId", producto.getId(),
                "productoNombre", producto.getNombre(),
                "stockTotal", stockTotal,
                "stockPorBodega", inventarios.stream()
                        .collect(Collectors.toMap(
                                inv -> inv.getBodega().getId().toString(),
                                inv -> inv.getStock(),
                                (a, b) -> a
                        ))
        );
        
        return result;
    }

    @Override
    public Bodega obtenerBodegaSugerida(Long productoId) {
        // Obtener inventarios del producto
        List<InventarioBodega> inventarios = inventarioBodegaRepository.findByProductoId(productoId);
        if (inventarios.isEmpty()) {
            return null;
        }
        
        // Encontrar la bodega con menor stock
        var bodegaMenorStock = inventarios.stream()
                .min(java.util.Comparator.comparing(inv -> inv.getStock() != null ? inv.getStock() : Integer.MAX_VALUE))
                .orElse(null);
        
        if (bodegaMenorStock != null) {
            return bodegaMenorStock.getBodega();
        }
        
        return null;
    }

    private void guardarAuditoria(TipoOperacion tipo, Producto producto, String valoresAnteriores, String valoresNuevos) {
        try {
            String username = UserContext.getUsername();
            if (username == null) {
                log.warn("No se pudo obtener usuario autenticado para auditoría de Producto");
                return;
            }

            Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);

            Auditoria audit = Auditoria.builder()
                    .tipoOperacion(tipo)
                    .fechaHora(LocalDateTime.now())
                    .usuario(usuario)
                    .entidadAfectada("Producto")
                    .entidadId(producto.getId())
                    .valoresAnteriores(valoresAnteriores)
                    .valoresNuevos(valoresNuevos)
                    .build();

            auditoriaRepository.save(audit);
            log.info("Auditoría guardada: {} en Producto id={}", tipo, producto.getId());
        } catch (Exception e) {
            log.error("Error guardando auditoría para Producto: {}", e.getMessage(), e);
        }
    }

    private String serializar(Object entity) {
        try {
            return objectMapper.writeValueAsString(entity);
        } catch (Exception e) {
            return entity.toString();
        }
    }
}
