package com.logitrack.service;

import com.logitrack.dto.ResumenPanelRequest;
import com.logitrack.exception.BadRequestException;
import com.logitrack.model.ResumenPanel;
import java.util.Optional;

public interface PanelResumenService {
    void publicar(ResumenPanelRequest request) throws BadRequestException;
    Optional<ResumenPanel> obtenerUltimoResumen();
}