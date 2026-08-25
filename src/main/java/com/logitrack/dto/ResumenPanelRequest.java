package com.logitrack.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
    private LocalDate fecha;
    private String narrativa;
    private List<AlertaDTO> alertas;
    private List<AccionSugeridaDTO> accionesSugeridas;
}