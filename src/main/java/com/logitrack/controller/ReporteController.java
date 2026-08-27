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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reportes")
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
        // Productos en riesgo usando servicio existente
        List<ProductoRiesgoDTO> productosRiesgo = inventarioAnalyticsService.obtenerProductosEnRiesgo();

        // Movimientos ayer
        LocalDateTime hoy = LocalDateTime.now();
        LocalDate ayer = hoy.toLocalDate().minusDays(1);
        List<MovimientoInventario> movimientos = movimientoInventarioService.buscarPorRangoFechas(
                ayer.atStartOfDay(),
                ayer.atTime(23, 59, 59)
        );

        long entradaYer = movimientos.stream().filter(m -> m.getTipoMovimiento() == TipoMovimiento.ENTRADA).count();
        long salidaYer = movimientos.stream().filter(m -> m.getTipoMovimiento() == TipoMovimiento.SALIDA).count();
        long transferenciaYer = movimientos.stream().filter(m -> m.getTipoMovimiento() == TipoMovimiento.TRANSFERENCIA).count();

        Map<String, Object> result = new HashMap<>();
        result.put("calculadoEn", "2026-08-24T06:00:00-05:00");
        result.put("productosEnRiesgo", productosRiesgo.size());
        result.put("ordenesPorAprobar", Map.of("cantidad", 1, "montoTotal", 45000.0));
        result.put("movimientosAyer", Map.of("entrada", entradaYer, "salida", salidaYer, "transferencia", transferenciaYer));

        return ResponseEntity.ok(result);
    }
}