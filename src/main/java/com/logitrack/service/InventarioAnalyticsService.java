package com.logitrack.service;

import com.logitrack.dto.ProductoRiesgoDTO;
import java.util.List;

public interface InventarioAnalyticsService {
    List<ProductoRiesgoDTO> obtenerProductosEnRiesgo();
}