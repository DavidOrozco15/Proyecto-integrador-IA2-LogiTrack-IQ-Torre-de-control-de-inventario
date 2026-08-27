package com.logitrack.controller;

import com.logitrack.dto.ProductoConInventarioDTO;
import com.logitrack.model.Producto;
import com.logitrack.model.Proveedor;
import com.logitrack.repository.ProveedorRepository;
import com.logitrack.service.ProductoService;
import com.logitrack.dto.ProductoRiesgoDTO;
import com.logitrack.service.InventarioAnalyticsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;
    private final InventarioAnalyticsService inventarioAnalyticsService;
    private final ProveedorRepository proveedorRepository;

    public ProductoController(ProductoService productoService, InventarioAnalyticsService inventarioAnalyticsService, ProveedorRepository proveedorRepository) {
        this.productoService = productoService;
        this.inventarioAnalyticsService = inventarioAnalyticsService;
        this.proveedorRepository = proveedorRepository;
    }

    @GetMapping
    public ResponseEntity<List<Producto>> obtenerTodos(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Boolean bajoStock) {
        if (nombre != null || categoria != null || bajoStock != null) {
            return ResponseEntity.ok(productoService.filtrarProductos(nombre, categoria, bajoStock));
        }
        return ResponseEntity.ok(productoService.obtenerTodos());
    }

    @GetMapping("/con-inventario")
    public ResponseEntity<List<ProductoConInventarioDTO>> obtenerTodosConInventario() {
        return ResponseEntity.ok(productoService.obtenerTodosConInventario());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Producto> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.obtenerPorId(id));
    }

    @GetMapping("/{id}/stock")
    public ResponseEntity<Map<String, Object>> obtenerStockPorProducto(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.obtenerStockPorProducto(id));
    }

    @GetMapping("/riesgo")
    public ResponseEntity<List<ProductoRiesgoDTO>> obtenerProductosEnRiesgo() {
        return ResponseEntity.ok(inventarioAnalyticsService.obtenerProductosEnRiesgo());
    }

    @GetMapping("/{id}/con-inventario")
    public ResponseEntity<ProductoConInventarioDTO> obtenerConInventarioPorId(@PathVariable Long id) {
        return ResponseEntity.ok(productoService.obtenerConInventarioPorId(id));
    }

    @GetMapping("/bajo-stock")
    public ResponseEntity<List<Producto>> obtenerBajoStock(@RequestParam(defaultValue = "10") Integer umbral) {
        return ResponseEntity.ok(productoService.buscarBajoStock(umbral));
    }

    @PostMapping
    public ResponseEntity<Producto> crear(@Valid @RequestBody Producto producto) {
        return new ResponseEntity<>(productoService.guardar(producto), HttpStatus.CREATED);
    }

    @PostMapping("/con-inventario")
    public ResponseEntity<Producto> crearConInventario(@Valid @RequestBody ProductoRequest request) {
        return new ResponseEntity<>(
            productoService.guardarConInventario(request.producto(), request.stockPorBodega()),
            HttpStatus.CREATED
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<Producto> actualizar(@PathVariable Long id, @Valid @RequestBody Producto producto) {
        return ResponseEntity.ok(productoService.actualizar(id, producto));
    }

    @PutMapping("/{id}/con-inventario")
    public ResponseEntity<Producto> actualizarConInventario(@PathVariable Long id, @Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.ok(
            productoService.actualizarConInventario(id, request.producto(), request.stockPorBodega())
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        productoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
@GetMapping("/proveedores")
    public ResponseEntity<List<Proveedor>> obtenerProveedores() {
        return ResponseEntity.ok(proveedorRepository.findAll());
    }
}

record ProductoRequest(Producto producto, Map<Long, Integer> stockPorBodega) {}
