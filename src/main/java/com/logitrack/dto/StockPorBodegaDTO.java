package com.logitrack.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockPorBodegaDTO {

    private Long bodegaId;
    private String bodegaNombre;
    private long stockTotal;
    private int capacidad;
    private double porcentajeOcupacion;
}
