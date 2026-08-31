package com.logitrack.controller;

import com.logitrack.exception.BadRequestException;
import com.logitrack.exception.ResourceNotFoundException;
import com.logitrack.model.EstadoOrdenCompra;
import com.logitrack.model.OrdenCompra;
import com.logitrack.service.OrdenCompraService;
import com.logitrack.service.PdfOrdenService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/ordenes")
public class OrdenCompraController {

    private final OrdenCompraService ordenCompraService;
    private final PdfOrdenService pdfOrdenService;

    public OrdenCompraController(OrdenCompraService ordenCompraService, PdfOrdenService pdfOrdenService) {
        this.ordenCompraService = ordenCompraService;
        this.pdfOrdenService = pdfOrdenService;
    }

    @GetMapping
    public ResponseEntity<List<OrdenCompra>> obtenerTodas(@RequestParam(required = false) EstadoOrdenCompra estado) {
        if (estado != null) {
            return ResponseEntity.ok(ordenCompraService.obtenerPorEstado(estado));
        }
        return ResponseEntity.ok(ordenCompraService.obtenerTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrdenCompra> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(ordenCompraService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<OrdenCompra> crear(@RequestBody OrdenCompra orden) throws BadRequestException {
        return new ResponseEntity<>(ordenCompraService.crear(orden), HttpStatus.CREATED);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrdenCompra> cambiarEstado(@PathVariable Long id,
                                                     @RequestBody Map<String, String> body) throws BadRequestException {
        EstadoOrdenCompra nuevoEstado = EstadoOrdenCompra.valueOf(body.get("estado"));
        return ResponseEntity.ok(ordenCompraService.cambiarEstado(id, nuevoEstado));
    }

    @PostMapping("/{id}/pdf")
    public ResponseEntity<byte[]> generarPdf(@PathVariable Long id) {
        byte[] pdf = pdfOrdenService.generarPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=orden-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> obtenerPdf(@PathVariable Long id) {
        byte[] pdf = pdfOrdenService.obtenerPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=orden-" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}