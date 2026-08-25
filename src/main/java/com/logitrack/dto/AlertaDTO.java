package com.logitrack.dto;

import com.logitrack.model.SeveridadAlerta;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlertaDTO {
    private SeveridadAlerta severidad;
    private String titulo;
    private String detalle;
    private Long productoId;
}