package com.logitrack.controller;

import com.logitrack.model.EstadoOrdenCompra;
import com.logitrack.model.MovimientoInventario;
import com.logitrack.model.TipoMovimiento;
import com.logitrack.dto.ProductoRiesgoDTO;
import com.logitrack.service.InventarioAnalyticsService;
import com.logitrack.service.MovimientoInventarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ReporteController {

    private final InventarioAnalyticsService inventarioAnalyticsService;
    private final MovimientoInventarioService movimientoInventarioService;

    public ReporteController(InventarioAnalyticsService inventarioAnalyticsService,
                             MovimientoInventarioService movimientoInventarioService) {
        this.inventarioAnalyticsService = inventarioAnalyticsService;
        this.movimientoInventarioService = movimientoInventarioService;
    }

    @GetMapping("/kpis")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENTE', 'EMPLEADO')")
    public ResponseEntity<Map<String, Object>> obtenerKPIs() {
        return ResponseEntity.ok(inventarioAnalyticsService.obtenerKPIsCompletos());
    }
}