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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class PdfOrdenService {

    private final OrdenCompraRepository ordenCompraRepository;

    private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Transactional
    public byte[] generarPdf(Long ordenId) {
        OrdenCompra orden = ordenCompraRepository.findById(ordenId)
                .orElseThrow(() -> new ResourceNotFoundException("OrdenCompra", "id", ordenId));

        byte[] pdf = crearPdfProfesional(orden);
        orden.setPdfBytes(pdf);
        orden.setPdfFechaGeneracion(java.time.LocalDateTime.now());
        ordenCompraRepository.save(orden);
        return pdf;
    }

    @Transactional(readOnly = true)
    public byte[] obtenerPdf(Long ordenId) {
        OrdenCompra orden = ordenCompraRepository.findById(ordenId)
                .orElseThrow(() -> new ResourceNotFoundException("OrdenCompra", "id", ordenId));

        if (orden.getPdfBytes() == null) {
            throw new ResourceNotFoundException("El PDF de la orden no esta disponible");
        }
        return orden.getPdfBytes();
    }

    private byte[] crearPdfProfesional(OrdenCompra orden) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                float pageWidth = PDRectangle.A4.getWidth();
                float pageHeight = PDRectangle.A4.getHeight();
                float margin = 40;
                float contentWidth = pageWidth - (margin * 2);

                // === HEADER ===
                fillRect(cs, 0, pageHeight - 80, pageWidth, 80, 30, 58, 95);

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 22);
                cs.setNonStrokingColor(255, 255, 255);
                cs.newLineAtOffset(margin, pageHeight - 45);
                cs.showText("LogiTrack IQ");
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.setNonStrokingColor(189, 195, 201);
                cs.newLineAtOffset(margin, pageHeight - 62);
                cs.showText("Torre de Control de Inventario");
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 14);
                cs.setNonStrokingColor(255, 255, 255);
                cs.newLineAtOffset(pageWidth - margin - 180, pageHeight - 42);
                cs.showText("ORDEN DE COMPRA");
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 9);
                cs.setNonStrokingColor(189, 195, 201);
                cs.newLineAtOffset(pageWidth - margin - 180, pageHeight - 58);
                cs.showText("Documento Oficial");
                cs.endText();

                // === RED LINE ===
                float yAfterHeader = pageHeight - 85;
                fillRect(cs, 0, yAfterHeader, pageWidth, 3, 231, 76, 60);

                // === INFO BOX: Number, Date, Status, Author ===
                float yInfoBox = yAfterHeader - 75;
                fillRect(cs, margin, yInfoBox, contentWidth, 65, 245, 247, 250);
                drawBorder(cs, margin, yInfoBox, contentWidth, 65, 44, 85, 130, 1);

                // Order number
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 8);
                cs.setNonStrokingColor(127, 140, 141);
                cs.newLineAtOffset(margin + 10, yInfoBox + 45);
                cs.showText("NUMERO DE ORDEN");
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 16);
                cs.setNonStrokingColor(30, 58, 95);
                cs.newLineAtOffset(margin + 10, yInfoBox + 22);
                cs.showText("#" + String.format("%06d", orden.getId()));
                cs.endText();

                // Date
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 8);
                cs.setNonStrokingColor(127, 140, 141);
                cs.newLineAtOffset(margin + 160, yInfoBox + 45);
                cs.showText("FECHA DE EMISION");
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
                cs.setNonStrokingColor(44, 62, 80);
                cs.newLineAtOffset(margin + 160, yInfoBox + 25);
                cs.showText(orden.getFechaCreacion() != null ? orden.getFechaCreacion().format(FECHA_FMT) : "N/A");
                cs.endText();

                // Status badge
                float estadoX = margin + contentWidth - 160;
                fillRect(cs, estadoX, yInfoBox + 18, 140, 35, 30, 58, 95);
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
                cs.setNonStrokingColor(255, 255, 255);
                cs.newLineAtOffset(estadoX + 15, yInfoBox + 28);
                cs.showText(orden.getEstado() != null ? orden.getEstado().toString() : "N/A");
                cs.endText();

                // Created by
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 8);
                cs.setNonStrokingColor(127, 140, 141);
                cs.newLineAtOffset(margin + 340, yInfoBox + 45);
                cs.showText("CREADO POR");
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
                cs.setNonStrokingColor(44, 62, 80);
                cs.newLineAtOffset(margin + 340, yInfoBox + 25);
                cs.showText(orden.getCreadoPor() != null ? orden.getCreadoPor() : "Sistema");
                cs.endText();

                // === PROVEEDOR + BODEGA ===
                float yBoxes = yInfoBox - 15;
                float boxH = 80;
                float halfW = (contentWidth - 10) / 2;

                // Proveedor box
                fillRect(cs, margin, yBoxes - boxH, halfW, boxH, 255, 255, 255);
                drawBorder(cs, margin, yBoxes - boxH, halfW, boxH, 44, 85, 130, 1);
                fillRect(cs, margin, yBoxes - 18, halfW, 18, 44, 85, 130);

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 8);
                cs.setNonStrokingColor(255, 255, 255);
                cs.newLineAtOffset(margin + 8, yBoxes - 14);
                cs.showText("PROVEEDOR");
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
                cs.setNonStrokingColor(44, 62, 80);
                cs.newLineAtOffset(margin + 10, yBoxes - 35);
                String provNombre = orden.getProveedor() != null ? orden.getProveedor().getNombre() : "N/A";
                cs.showText(provNombre.length() > 35 ? provNombre.substring(0, 35) : provNombre);
                cs.endText();

                if (orden.getProveedor() != null && orden.getProveedor().getContacto() != null) {
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 9);
                    cs.setNonStrokingColor(127, 140, 141);
                    cs.newLineAtOffset(margin + 10, yBoxes - 50);
                    cs.showText("Contacto: " + orden.getProveedor().getContacto());
                    cs.endText();
                }

                if (orden.getProveedor() != null && orden.getProveedor().getTelefono() != null) {
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 9);
                    cs.setNonStrokingColor(127, 140, 141);
                    cs.newLineAtOffset(margin + 10, yBoxes - 63);
                    cs.showText("Tel: " + orden.getProveedor().getTelefono());
                    cs.endText();
                }

                if (orden.getProveedor() != null) {
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 9);
                    cs.setNonStrokingColor(127, 140, 141);
                    cs.newLineAtOffset(margin + halfW / 2 + 10, yBoxes - 63);
                    cs.showText("Entrega: " + orden.getProveedor().getDiasEntrega() + " dias");
                    cs.endText();
                }

                // Bodega box
                float bodegaX = margin + halfW + 10;
                fillRect(cs, bodegaX, yBoxes - boxH, halfW, boxH, 255, 255, 255);
                drawBorder(cs, bodegaX, yBoxes - boxH, halfW, boxH, 44, 85, 130, 1);
                fillRect(cs, bodegaX, yBoxes - 18, halfW, 18, 44, 85, 130);

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 8);
                cs.setNonStrokingColor(255, 255, 255);
                cs.newLineAtOffset(bodegaX + 8, yBoxes - 14);
                cs.showText("BODEGA DESTINO");
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
                cs.setNonStrokingColor(44, 62, 80);
                cs.newLineAtOffset(bodegaX + 10, yBoxes - 35);
                cs.showText(orden.getBodegaDestino() != null ? orden.getBodegaDestino().getNombre() : "N/A");
                cs.endText();

                if (orden.getBodegaDestino() != null && orden.getBodegaDestino().getUbicacion() != null) {
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 9);
                    cs.setNonStrokingColor(127, 140, 141);
                    cs.newLineAtOffset(bodegaX + 10, yBoxes - 50);
                    String ubi = orden.getBodegaDestino().getUbicacion();
                    cs.showText("Ubicacion: " + (ubi.length() > 30 ? ubi.substring(0, 30) : ubi));
                    cs.endText();
                }

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 9);
                cs.setNonStrokingColor(127, 140, 141);
                cs.newLineAtOffset(bodegaX + 10, yBoxes - 63);
                cs.showText("Capacidad: " + (orden.getBodegaDestino() != null ? orden.getBodegaDestino().getCapacidad() : "N/A"));
                cs.endText();

                // === PRODUCT TABLE ===
                float yTabla = yBoxes - boxH - 25;
                fillRect(cs, margin, yTabla - 22, contentWidth, 22, 30, 58, 95);

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 8);
                cs.setNonStrokingColor(255, 255, 255);
                cs.newLineAtOffset(margin + 8, yTabla - 17);
                cs.showText("PRODUCTO");
                cs.newLineAtOffset(200, 0);
                cs.showText("CATEGORIA");
                cs.newLineAtOffset(100, 0);
                cs.showText("CANTIDAD");
                cs.newLineAtOffset(70, 0);
                cs.showText("PRECIO UNIT.");
                cs.newLineAtOffset(80, 0);
                cs.showText("SUBTOTAL");
                cs.endText();

                // Product row
                float yFila = yTabla - 22;
                float filaH = 30;
                fillRect(cs, margin, yFila - filaH, contentWidth, filaH, 255, 255, 255);

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
                cs.setNonStrokingColor(44, 62, 80);
                cs.newLineAtOffset(margin + 8, yFila - 14);
                String prodNombre = orden.getProducto() != null ? orden.getProducto().getNombre() : "N/A";
                cs.showText(prodNombre.length() > 30 ? prodNombre.substring(0, 30) : prodNombre);
                cs.endText();

                if (orden.getProducto() != null && orden.getProducto().getCategoria() != null) {
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 8);
                    cs.setNonStrokingColor(127, 140, 141);
                    cs.newLineAtOffset(margin + 8, yFila - 25);
                    cs.showText(orden.getProducto().getCategoria());
                    cs.endText();
                }

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.setNonStrokingColor(44, 62, 80);
                cs.newLineAtOffset(margin + 208, yFila - 14);
                cs.showText(String.valueOf(orden.getCantidad()));
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.setNonStrokingColor(44, 62, 80);
                cs.newLineAtOffset(margin + 278, yFila - 14);
                cs.showText("$" + fmt(orden.getPrecioUnitario()));
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
                cs.setNonStrokingColor(30, 58, 95);
                cs.newLineAtOffset(margin + 358, yFila - 14);
                cs.showText("$" + fmt(orden.getTotal()));
                cs.endText();

                // === TOTALS ===
                float yTot = yFila - filaH - 15;
                float totW = 220;
                float totX = margin + contentWidth - totW;

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.setNonStrokingColor(44, 62, 80);
                cs.newLineAtOffset(totX, yTot);
                cs.showText("Subtotal:");
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.setNonStrokingColor(44, 62, 80);
                cs.newLineAtOffset(totX + 120, yTot);
                cs.showText("$" + fmt(orden.getTotal()));
                cs.endText();

                BigDecimal iva = orden.getTotal().multiply(new BigDecimal("0.19")).setScale(2, RoundingMode.HALF_UP);
                BigDecimal totalConIva = orden.getTotal().add(iva);

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.setNonStrokingColor(44, 62, 80);
                cs.newLineAtOffset(totX, yTot - 18);
                cs.showText("IVA (19%):");
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 10);
                cs.setNonStrokingColor(44, 62, 80);
                cs.newLineAtOffset(totX + 120, yTot - 18);
                cs.showText("$" + fmt(iva));
                cs.endText();

                fillRect(cs, totX, yTot - 30, totW, 1, 44, 85, 130);

                fillRect(cs, totX, yTot - 52, totW, 22, 30, 58, 95);
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
                cs.setNonStrokingColor(255, 255, 255);
                cs.newLineAtOffset(totX + 8, yTot - 47);
                cs.showText("TOTAL");
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 13);
                cs.setNonStrokingColor(255, 255, 255);
                cs.newLineAtOffset(totX + 100, yTot - 48);
                cs.showText("$" + fmt(totalConIva));
                cs.endText();

                // === TERMS ===
                float yObs = yTot - 75;
                fillRect(cs, margin, yObs - 50, contentWidth, 50, 245, 247, 250);
                drawBorder(cs, margin, yObs - 50, contentWidth, 50, 44, 85, 130, 1);

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 8);
                cs.setNonStrokingColor(44, 85, 130);
                cs.newLineAtOffset(margin + 8, yObs - 12);
                cs.showText("TERMINOS Y CONDICIONES");
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 9);
                cs.setNonStrokingColor(44, 62, 80);
                cs.newLineAtOffset(margin + 8, yObs - 28);
                cs.showText("Orden de compra sujeta a disponibilidad de stock y politicas de la empresa.");
                cs.endText();

                // === SIGNATURES ===
                float yFirmas = yObs - 75;
                float firmaW = (contentWidth - 40) / 3;

                // Solicitante
                fillRect(cs, margin, yFirmas - 40, firmaW, 1, 44, 62, 80);
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 8);
                cs.setNonStrokingColor(127, 140, 141);
                cs.newLineAtOffset(margin + firmaW / 2 - 40, yFirmas - 50);
                cs.showText("Firma del Solicitante");
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 7);
                cs.setNonStrokingColor(127, 140, 141);
                cs.newLineAtOffset(margin + firmaW / 2 - 55, yFirmas - 60);
                cs.showText(orden.getCreadoPor() != null ? orden.getCreadoPor() : "N/A");
                cs.endText();

                // Aprobacion
                float firma2X = margin + firmaW + 20;
                fillRect(cs, firma2X, yFirmas - 40, firmaW, 1, 44, 62, 80);
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 8);
                cs.setNonStrokingColor(127, 140, 141);
                cs.newLineAtOffset(firma2X + firmaW / 2 - 35, yFirmas - 50);
                cs.showText("Firma de Aprobacion");
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 7);
                cs.setNonStrokingColor(127, 140, 141);
                cs.newLineAtOffset(firma2X + firmaW / 2 - 55, yFirmas - 60);
                cs.showText("Gerente de Almacen");
                cs.endText();

                // Recepcion
                float firma3X = margin + (firmaW + 20) * 2;
                fillRect(cs, firma3X, yFirmas - 40, firmaW, 1, 44, 62, 80);
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 8);
                cs.setNonStrokingColor(127, 140, 141);
                cs.newLineAtOffset(firma3X + firmaW / 2 - 30, yFirmas - 50);
                cs.showText("Firma de Recepcion");
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 7);
                cs.setNonStrokingColor(127, 140, 141);
                cs.newLineAtOffset(firma3X + firmaW / 2 - 55, yFirmas - 60);
                cs.showText("Encargado de Bodega");
                cs.endText();

                // === FOOTER ===
                fillRect(cs, 0, 0, pageWidth, 35, 30, 58, 95);

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 7);
                cs.setNonStrokingColor(189, 195, 201);
                cs.newLineAtOffset(margin, 15);
                cs.showText("LogiTrack IQ - Torre de Control de Inventario | Generado automaticamente");
                cs.endText();

                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 7);
                cs.setNonStrokingColor(189, 195, 201);
                cs.newLineAtOffset(pageWidth - margin - 80, 15);
                cs.showText("Pagina 1 de 1");
                cs.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error al generar el PDF de la orden", e);
        }
    }

    private void fillRect(PDPageContentStream cs, float x, float y, float w, float h,
                          int r, int g, int b) throws IOException {
        cs.setNonStrokingColor(r, g, b);
        cs.addRect(x, y, w, h);
        cs.fill();
    }

    private void drawBorder(PDPageContentStream cs, float x, float y, float w, float h,
                            int r, int g, int b, float strokeWidth) throws IOException {
        cs.setStrokingColor(r, g, b);
        cs.setLineWidth(strokeWidth);
        cs.addRect(x, y, w, h);
        cs.stroke();
    }

    private String fmt(BigDecimal amount) {
        if (amount == null) return "0.00";
        return String.format("%,.2f", amount);
    }
}
