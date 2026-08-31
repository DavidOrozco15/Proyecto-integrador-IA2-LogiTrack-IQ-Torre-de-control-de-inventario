package com.logitrack.controller;

import com.logitrack.model.Bodega;
import com.logitrack.model.EstadoOrdenCompra;
import com.logitrack.model.OrdenCompra;
import com.logitrack.model.Producto;
import com.logitrack.model.Proveedor;
import com.logitrack.repository.BodegaRepository;
import com.logitrack.repository.OrdenCompraRepository;
import com.logitrack.repository.ProductoRepository;
import com.logitrack.repository.ProveedorRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Caso 8 de docs/sdd/02-especificacion.md: el PDF de una orden en BORRADOR se
 * genera con la marca de agua "BORRADOR"; al cambiar el estado, el PDF deja de
 * estar disponible (404) hasta que se regenera.
 *
 * Los datos base (proveedor, bodega, producto, orden) se insertan directamente
 * vía repositorios JPA contra la base H2 de test — no dependemos del flujo REST
 * de creación para mantener este test enfocado en el PDF.
 *
 * ESTADO: escrito en Día 2 (rojo). Requiere OrdenCompra, EstadoOrdenCompra,
 * PdfOrdenService y los endpoints POST/GET /api/ordenes/{id}/pdf — nada de esto
 * existe todavía. Requiere también la dependencia org.apache.pdfbox:pdfbox.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrdenCompraPdfIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ProveedorRepository proveedorRepository;
    @Autowired private BodegaRepository bodegaRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private OrdenCompraRepository ordenCompraRepository;

    private Long ordenId;

    @BeforeEach
    void seedOrdenBorrador() {
        Proveedor proveedor = proveedorRepository.save(
                Proveedor.builder().nombre("Proveedor PDF Test").diasEntrega(7).build());
        Bodega bodega = bodegaRepository.save(
                Bodega.builder().nombre("Bodega PDF Test").ubicacion("Test").capacidad(1000).build());
        Producto producto = productoRepository.save(
                Producto.builder().nombre("Producto PDF Test").stock(0).precio(BigDecimal.TEN).build());

        OrdenCompra orden = ordenCompraRepository.save(OrdenCompra.builder()
                .producto(producto)
                .proveedor(proveedor)
                .bodegaDestino(bodega)
                .cantidad(10)
                .precioUnitario(BigDecimal.TEN)
                .total(BigDecimal.valueOf(100))
                .estado(EstadoOrdenCompra.BORRADOR)
                .build());

        ordenId = orden.getId();
    }

    @Test
    @WithMockUser(username = "admin-test", roles = {"ADMIN"})
    void pdfDeOrdenBorrador_contieneMarcaDeAgua_yDesapareceAlCancelar() throws Exception {
        // 1. Generar el PDF de la orden en BORRADOR
        mockMvc.perform(post("/api/ordenes/{id}/pdf", ordenId))
                .andExpect(status().is2xxSuccessful());

        // 2. Descargarlo y verificar que el texto "BORRADOR" está presente
        byte[] pdfBytes = mockMvc.perform(get("/api/ordenes/{id}/pdf", ordenId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        String textoExtraido;
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes))) {
            textoExtraido = new PDFTextStripper().getText(document);
        }
        assertThat(textoExtraido).contains("BORRADOR");

        // 3. Cambiar el estado a CANCELADA
        mockMvc.perform(patch("/api/ordenes/{id}/estado", ordenId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"CANCELADA\"}"))
                .andExpect(status().is2xxSuccessful());

        // 4. El PDF anterior ya no debe estar disponible
        mockMvc.perform(get("/api/ordenes/{id}/pdf", ordenId))
                .andExpect(status().isNotFound());
    }
}
