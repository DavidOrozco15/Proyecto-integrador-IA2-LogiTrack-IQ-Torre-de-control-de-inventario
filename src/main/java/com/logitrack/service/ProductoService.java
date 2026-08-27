package com.logitrack.service;

import com.logitrack.dto.ProductoConInventarioDTO;
import com.logitrack.model.Producto;

import java.util.List;
import java.util.Map;

public interface ProductoService {

    List<Producto> obtenerTodos();

    Producto obtenerPorId(Long id);

    Producto guardar(Producto producto);

    Producto actualizar(Long id, Producto producto);

    void eliminar(Long id);

    List<Producto> buscarPorNombre(String nombre);

    List<Producto> buscarBajoStock(Integer umbral);

    List<Producto> filtrarProductos(String nombre, String categoria, Boolean bajoStock);

    Producto guardarConInventario(Producto producto, Map<Long, Integer> stockPorBodega);

    Producto actualizarConInventario(Long id, Producto producto, Map<Long, Integer> stockPorBodega);

    List<ProductoConInventarioDTO> obtenerTodosConInventario();

    ProductoConInventarioDTO obtenerConInventarioPorId(Long id);

    Map<String, Object> obtenerStockPorProducto(Long id);
}
