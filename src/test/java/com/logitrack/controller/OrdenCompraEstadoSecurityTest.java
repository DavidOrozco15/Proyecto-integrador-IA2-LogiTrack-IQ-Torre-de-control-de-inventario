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

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Caso 6 de docs/sdd/02-especificacion.md: un usuario con rol AGENTE nunca puede
 * aprobar/cancelar/recibir una orden — PATCH /api/ordenes/{id}/estado está
 * protegido con hasRole("ADMIN") en dos capas (SecurityConfig + @PreAuthorize).
 *
 * Se usa @WithMockUser para simular el rol sin pasar por el login JWT real; la
 * capa de autorización de Spring Security reacciona igual sin importar cómo se
 * estableció la autenticación, así que el 403 esperado es una prueba válida.
 *
 * ESTADO: escrito en Día 2 (rojo). Requiere Rol.AGENTE, OrdenCompraController con
 * el endpoint PATCH /api/ordenes/{id}/estado, y las reglas nuevas en
 * SecurityConfig — nada de esto existe todavía, así que el contexto de Spring
 * puede incluso fallar al arrancar (también cuenta como "rojo").
 *
 * Requiere la dependencia de test spring-security-test para @WithMockUser
 * (ver nota en la respuesta del asistente sobre qué agregar al pom.xml).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrdenCompraEstadoSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "agente-test", roles = {"AGENTE"})
    void agenteIntentaCambiarEstadoDeUnaOrden_respondeForbidden() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("estado", "APROBADA"));

        // El id de la orden es irrelevante: la autorización por rol debe
        // rechazar la petición antes de que el controlador siquiera busque la
        // orden en base de datos.
        mockMvc.perform(patch("/api/ordenes/{id}/estado", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }
}
