package com.logitrack.service;

import com.logitrack.dto.AccionSugeridaDTO;
import com.logitrack.dto.AlertaDTO;
import com.logitrack.dto.ResumenPanelRequest;
import com.logitrack.exception.BadRequestException;
import com.logitrack.model.SeveridadAlerta;
import com.logitrack.model.TipoAccionSugerida;
import com.logitrack.repository.AuditoriaRepository;
import com.logitrack.repository.BodegaRepository;
import com.logitrack.repository.OrdenCompraRepository;
import com.logitrack.repository.ProductoRepository;
import com.logitrack.repository.ProveedorRepository;
import com.logitrack.repository.ResumenPanelRepository;
import com.logitrack.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cubre el caso 7 de docs/sdd/02-especificacion.md: un resumen que referencia un
 * identificador inexistente (producto, orden o bodega) debe responder 400 y el
 * resumen anterior NO debe modificarse — por eso verificamos que
 * ResumenPanelRepository.save(...) nunca se invoque.
 *
 * ESTADO: escrito en Día 2 (rojo). Requiere ResumenPanelRequest, AlertaDTO,
 * AccionSugeridaDTO, SeveridadAlerta, TipoAccionSugerida, ResumenPanelRepository,
 * PanelResumenService / PanelResumenServiceImpl — ninguno existe todavía.
 */
@ExtendWith(MockitoExtension.class)
class PanelResumenServiceTest {

    @Mock private ProveedorRepository proveedorRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private BodegaRepository bodegaRepository;
    @Mock private OrdenCompraRepository ordenCompraRepository;
    @Mock private ResumenPanelRepository resumenPanelRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AuditoriaRepository auditoriaRepository;

    @InjectMocks
    private PanelResumenServiceImpl panelResumenService;

    @Test
    void publicar_conProductoIdInexistenteEnUnaAlerta_lanzaBadRequest_yNoGuardaNada() {
        AlertaDTO alerta = new AlertaDTO();
        alerta.setSeveridad(SeveridadAlerta.ALTA);
        alerta.setTitulo("Producto en riesgo");
        alerta.setDetalle("Detalle de prueba con contenido suficiente.");
        alerta.setProductoId(999L); // no existe

        ResumenPanelRequest request = new ResumenPanelRequest();
        request.setFecha(LocalDate.now());
        request.setNarrativa("Narrativa de prueba con longitud suficiente para pasar la validacion.");
        request.setAlertas(List.of(alerta));
        request.setAccionesSugeridas(List.of());

        when(productoRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> panelResumenService.publicar(request))
                .isInstanceOf(BadRequestException.class);

        verify(resumenPanelRepository, never()).save(any());
    }

    @Test
    void publicar_conOrdenIdInexistenteEnUnaAccion_lanzaBadRequest_yNoGuardaNada() {
        AccionSugeridaDTO accion = new AccionSugeridaDTO();
        accion.setTipo(TipoAccionSugerida.REVISAR_ORDEN);
        accion.setDescripcion("Revisar una orden que no existe.");
        accion.setOrdenId(999L); // no existe

        ResumenPanelRequest request = new ResumenPanelRequest();
        request.setFecha(LocalDate.now());
        request.setNarrativa("Narrativa de prueba con longitud suficiente para pasar la validacion.");
        request.setAlertas(List.of());
        request.setAccionesSugeridas(List.of(accion));

        when(ordenCompraRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> panelResumenService.publicar(request))
                .isInstanceOf(BadRequestException.class);

        verify(resumenPanelRepository, never()).save(any());
    }
}
