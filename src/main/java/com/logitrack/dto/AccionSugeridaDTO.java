package com.logitrack.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.logitrack.model.TipoAccionSugerida;
import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = false)
public class AccionSugeridaDTO {
    private TipoAccionSugerida tipo;
    private String descripcion;
    private Long ordenId;
    private Long productoId;
    private Long bodegaId;

    @AssertTrue(message = "Una accion sugerida debe enlazar exactamente un identificador")
    public boolean isExactamenteUnIdentificador() {
        int count = 0;
        if (productoId != null) count++;
        if (ordenId != null) count++;
        if (bodegaId != null) count++;
        return count == 1;
    }
}