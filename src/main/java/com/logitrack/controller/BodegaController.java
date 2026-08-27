package com.logitrack.controller;

import com.logitrack.dto.StockPorBodegaDTO;
import com.logitrack.model.Bodega;
import com.logitrack.model.InventarioBodega;
import com.logitrack.service.BodegaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bodegas")
public class BodegaController {

    private final BodegaService bodegaService;

    public BodegaController(BodegaService bodegaService) {
        this.bodegaService = bodegaService;
    }

    @GetMapping
    public ResponseEntity<List<Bodega>> obtenerTodas() {
        return ResponseEntity.ok(bodegaService.obtenerTodas());
    }

    @GetMapping("/stock")
    public ResponseEntity<List<StockPorBodegaDTO>> obtenerStockDeTodas() {
        return ResponseEntity.ok(bodegaService.obtenerStockTodas());
    }

    @GetMapping("/criticas")
    public ResponseEntity<List<Bodega>> obtenerCriticas() {
        return ResponseEntity.ok(bodegaService.obtenerCriticas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Bodega> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(bodegaService.obtenerPorId(id));
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<Bodega>> buscarPorNombre(@RequestParam String nombre) {
        return ResponseEntity.ok(bodegaService.buscarPorNombre(nombre));
    }

    @PostMapping
    public ResponseEntity<Bodega> crear(@Valid @RequestBody Bodega bodega) {
        return new ResponseEntity<>(bodegaService.guardar(bodega), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Bodega> actualizar(@PathVariable Long id, @Valid @RequestBody Bodega bodega) {
        return ResponseEntity.ok(bodegaService.actualizar(id, bodega));
    }

    @GetMapping("/{id}/inventario")
    public ResponseEntity<List<InventarioBodega>> obtenerInventario(@PathVariable Long id) {
        return ResponseEntity.ok(bodegaService.obtenerInventarioPorBodega(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        bodegaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
