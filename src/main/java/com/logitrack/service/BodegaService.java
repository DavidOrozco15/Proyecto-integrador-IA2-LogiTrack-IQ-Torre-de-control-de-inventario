package com.logitrack.service;

import com.logitrack.dto.StockPorBodegaDTO;
import com.logitrack.model.Bodega;
import com.logitrack.model.InventarioBodega;

import java.util.List;

public interface BodegaService {

    List<Bodega> obtenerTodas();

    Bodega obtenerPorId(Long id);

    Bodega guardar(Bodega bodega);

    Bodega actualizar(Long id, Bodega bodega);

    void eliminar(Long id);

    List<Bodega> buscarPorNombre(String nombre);

    List<InventarioBodega> obtenerInventarioPorBodega(Long bodegaId);

    List<StockPorBodegaDTO> obtenerStockTodas();

    List<Bodega> obtenerCriticas();
}
