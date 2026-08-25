package com.logitrack.service;

import com.logitrack.exception.ResourceNotFoundException;
import com.logitrack.model.OrdenCompra;
import com.logitrack.repository.OrdenCompraRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@RequiredArgsConstructor
public class PdfOrdenService {

    private final OrdenCompraRepository ordenCompraRepository;

    @Transactional
    public byte[] generarPdf(Long ordenId) {
        OrdenCompra orden = ordenCompraRepository.findById(ordenId)
                .orElseThrow(() -> new ResourceNotFoundException("OrdenCompra", "id", ordenId));

        byte[] pdf = crearPdfConMarcaDeAgua(orden);
        orden.setPdfBytes(pdf);
        ordenCompraRepository.save(orden);
        return pdf;
    }

    @Transactional(readOnly = true)
    public byte[] obtenerPdf(Long ordenId) {
        OrdenCompra orden = ordenCompraRepository.findById(ordenId)
                .orElseThrow(() -> new ResourceNotFoundException("OrdenCompra", "id", ordenId));

        if (orden.getPdfBytes() == null) {
            throw new ResourceNotFoundException("El PDF de la orden no está disponible");
        }
        return orden.getPdfBytes();
    }

    private byte[] crearPdfConMarcaDeAgua(OrdenCompra orden) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                // Título
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
                cs.newLineAtOffset(50, 750);
                cs.showText("ORDEN DE COMPRA #" + orden.getId());
                cs.endText();

                // Datos de la orden
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 12);
                cs.newLineAtOffset(50, 720);
                cs.showText("Producto: " + (orden.getProducto() != null ? orden.getProducto().getNombre() : "-"));
                cs.newLineAtOffset(0, -20);
                cs.showText("Proveedor: " + (orden.getProveedor() != null ? orden.getProveedor().getNombre() : "-"));
                cs.newLineAtOffset(0, -20);
                cs.showText("Cantidad: " + orden.getCantidad());
                cs.newLineAtOffset(0, -20);
                cs.showText("Precio unitario: " + orden.getPrecioUnitario());
                cs.newLineAtOffset(0, -20);
                cs.showText("Total: " + orden.getTotal());
                cs.endText();

                // Marca de agua "BORRADOR" como texto real (extraíble con PDFTextStripper)
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 60);
                cs.setNonStrokingColor(200, 200, 200);
                cs.newLineAtOffset(120, 400);
                cs.showText("BORRADOR");
                cs.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error al generar el PDF de la orden", e);
        }
    }
}