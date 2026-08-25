package com.logitrack.dto;

import com.logitrack.model.TipoAccionSugerida;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccionSugeridaDTO {
    private TipoAccionSugerida tipo;
    private String descripcion;
    private Long ordenId;
}