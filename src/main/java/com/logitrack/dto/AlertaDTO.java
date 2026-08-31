package com.logitrack.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.logitrack.model.SeveridadAlerta;
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
public class AlertaDTO {
    private SeveridadAlerta severidad;
    private String titulo;
    private String detalle;
    private Long productoId;
    private Long ordenId;
    private Long bodegaId;

    @AssertTrue(message = "Una alerta debe enlazar al menos un identificador")
    public boolean isAlMenosUnIdentificador() {
        return productoId != null || ordenId != null || bodegaId != null;
    }
}