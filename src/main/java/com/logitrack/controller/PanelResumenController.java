package com.logitrack.controller;

import com.logitrack.dto.ResumenPanelRequest;
import com.logitrack.exception.BadRequestException;
import com.logitrack.model.ResumenPanel;
import com.logitrack.service.PanelResumenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/panel")
public class PanelResumenController {

    @Autowired
    private PanelResumenService panelResumenService;

    @PostMapping("/resumen")
    public ResumenPanel publicarResumen(@RequestBody ResumenPanelRequest request) throws BadRequestException {
        panelResumenService.publicar(request);
        return panelResumenService.obtenerUltimoResumen().orElse(null);
    }

    @GetMapping("/resumen")
    public ResumenPanel obtenerUltimoResumen() {
        return panelResumenService.obtenerUltimoResumen()
                .orElse(null);
    }
}