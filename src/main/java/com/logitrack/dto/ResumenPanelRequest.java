package com.logitrack.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class ResumenPanelRequest {
    private LocalDate fecha;
    private String narrativa;
    private List<AlertaDTO> alertas;
    private List<AccionSugeridaDTO> accionesSugeridas;
}