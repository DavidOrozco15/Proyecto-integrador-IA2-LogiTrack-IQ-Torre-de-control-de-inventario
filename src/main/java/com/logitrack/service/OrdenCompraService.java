package com.logitrack.service;

import com.logitrack.exception.BadRequestException;
import com.logitrack.model.EstadoOrdenCompra;
import com.logitrack.model.OrdenCompra;

public interface OrdenCompraService {
    OrdenCompra crear(OrdenCompra orden) throws BadRequestException;
    OrdenCompra cambiarEstado(Long id, EstadoOrdenCompra nuevoEstado) throws BadRequestException;
}