package com.logitrack.service;

import com.logitrack.exception.BadRequestException;
import com.logitrack.exception.ResourceNotFoundException;
import com.logitrack.model.EstadoOrdenCompra;
import com.logitrack.model.OrdenCompra;

import java.util.List;
import java.util.Optional;

public interface OrdenCompraService {
    OrdenCompra crear(OrdenCompra orden) throws BadRequestException;
    OrdenCompra cambiarEstado(Long id, EstadoOrdenCompra nuevoEstado) throws BadRequestException;
    List<OrdenCompra> obtenerTodas();
    List<OrdenCompra> obtenerPorEstado(EstadoOrdenCompra estado);
    OrdenCompra obtenerPorId(Long id) throws ResourceNotFoundException;
}