package com.logitrack.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = false)
public class ResumenPanelRequest {
    @NotNull(message = "La fecha es requerida")
    private LocalDate fecha;

    @NotBlank(message = "La narrativa es requerida")
    @Size(min = 20, max = 500, message = "La narrativa debe tener entre 20 y 500 caracteres")
    private String narrativa;

    @NotNull(message = "Las alertas son requeridas")
    @Valid
    private List<AlertaDTO> alertas;

    @NotNull(message = "Las acciones sugeridas son requeridas")
    @Valid
    private List<AccionSugeridaDTO> accionesSugeridas;
}
