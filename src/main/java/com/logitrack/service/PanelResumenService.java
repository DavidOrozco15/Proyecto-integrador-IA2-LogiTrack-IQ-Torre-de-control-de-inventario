package com.logitrack.service;

import com.logitrack.dto.ResumenPanelRequest;
import com.logitrack.exception.BadRequestException;

public interface PanelResumenService {
    void publicar(ResumenPanelRequest request) throws BadRequestException;
}