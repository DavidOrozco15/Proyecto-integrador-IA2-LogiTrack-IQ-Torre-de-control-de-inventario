package com.logitrack.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoRiesgoDTO {
    private Long productoId;
    private String nombre;
    private String categoria;
    private Integer stockTotal;
    private Integer puntoReorden;
    private Double consumoDiarioPromedio;
    private Double diasCobertura;
    private String estado;
    private String proveedor;
    private Long proveedorId;
    private Long bodegaDestinoId;
    private BigDecimal precioUnitario;
}