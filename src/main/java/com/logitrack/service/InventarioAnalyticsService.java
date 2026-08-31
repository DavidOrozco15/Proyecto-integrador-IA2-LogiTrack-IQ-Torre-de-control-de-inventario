package com.logitrack.service;

import com.logitrack.dto.ProductoRiesgoDTO;
import com.logitrack.dto.StockPorBodegaDTO;
import java.util.List;
import java.util.Map;

public interface InventarioAnalyticsService {
    List<ProductoRiesgoDTO> obtenerProductosEnRiesgo();
    List<ProductoRiesgoDTO> obtenerProductosEnQuiebre();
    List<StockPorBodegaDTO> obtenerOcupacionPorBodega();
    Map<String, Object> obtenerKPIsCompletos();
}