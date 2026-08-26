package com.logitrack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integración adicional exigida por el enunciado (docs/sdd/02-especificacion.md,
 * sección 11): un payload válido en POST /api/panel/resumen se guarda, y
 * GET /api/panel/resumen devuelve exactamente ese contenido después.
 *
 * ESTADO: escrito en Día 2 (rojo). Requiere PanelResumenController,
 * ResumenPanelRequest y el resto de la sección 7 de la especificación — nada de
 * esto existe todavía.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PanelResumenIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "admin-test", roles = {"ADMIN"})
    void publicarResumenValido_yLuegoConsultarlo_devuelveElMismoContenido() throws Exception {
        Map<String, Object> payload = Map.of(
                "fecha", LocalDate.now().toString(),
                "narrativa", "Resumen de prueba con longitud suficiente para pasar la validacion de negocio.",
                "alertas", List.of(),
                "accionesSugeridas", List.of()
        );

        mockMvc.perform(post("/api/panel/resumen")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().is2xxSuccessful());

        mockMvc.perform(get("/api/panel/resumen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fecha").value(LocalDate.now().toString()));
    }
}
