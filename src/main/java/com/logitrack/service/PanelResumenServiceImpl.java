package com.logitrack.service;

import com.logitrack.dto.ResumenPanelRequest;
import com.logitrack.exception.BadRequestException;
import com.logitrack.model.Auditoria;
import com.logitrack.model.ResumenPanel;
import com.logitrack.model.TipoOperacion;
import com.logitrack.model.Usuario;
import com.logitrack.config.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import com.logitrack.repository.AuditoriaRepository;
import com.logitrack.repository.BodegaRepository;
import com.logitrack.repository.OrdenCompraRepository;
import com.logitrack.repository.ProductoRepository;
import com.logitrack.repository.ProveedorRepository;
import com.logitrack.repository.ResumenPanelRepository;
import com.logitrack.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PanelResumenServiceImpl implements PanelResumenService {

    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;
    private final BodegaRepository bodegaRepository;
    private final OrdenCompraRepository ordenCompraRepository;
    private final ResumenPanelRepository resumenPanelRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaRepository auditoriaRepository;

    @Override
    @Transactional
    public void publicar(ResumenPanelRequest request) throws BadRequestException {
        if (request.getAlertas() != null) {
            for (var alerta : request.getAlertas()) {
                if (alerta.getProductoId() != null && !productoRepository.existsById(alerta.getProductoId())) {
                    throw new BadRequestException("Producto no encontrado: " + alerta.getProductoId());
                }
                if (alerta.getOrdenId() != null && !ordenCompraRepository.existsById(alerta.getOrdenId())) {
                    throw new BadRequestException("Orden no encontrada: " + alerta.getOrdenId());
                }
                if (alerta.getBodegaId() != null && !bodegaRepository.existsById(alerta.getBodegaId())) {
                    throw new BadRequestException("Bodega no encontrada: " + alerta.getBodegaId());
                }
            }
        }
        if (request.getAccionesSugeridas() != null) {
            for (var accion : request.getAccionesSugeridas()) {
                if (accion.getProductoId() != null && !productoRepository.existsById(accion.getProductoId())) {
                    throw new BadRequestException("Producto no encontrado: " + accion.getProductoId());
                }
                if (accion.getOrdenId() != null && !ordenCompraRepository.existsById(accion.getOrdenId())) {
                    throw new BadRequestException("Orden no encontrada: " + accion.getOrdenId());
                }
                if (accion.getBodegaId() != null && !bodegaRepository.existsById(accion.getBodegaId())) {
                    throw new BadRequestException("Bodega no encontrada: " + accion.getBodegaId());
                }
            }
        }

        String json;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"fecha\":\"").append(request.getFecha()).append("\",");
            sb.append("\"narrativa\":\"").append(escapeJson(request.getNarrativa())).append("\",");

            // Alertas
            sb.append("\"alertas\":[");
            if (request.getAlertas() != null) {
                for (int i = 0; i < request.getAlertas().size(); i++) {
                    var a = request.getAlertas().get(i);
                    if (i > 0) sb.append(",");
                    sb.append("{");
                    sb.append("\"severidad\":\"").append(a.getSeveridad()).append("\",");
                    sb.append("\"titulo\":\"").append(escapeJson(a.getTitulo())).append("\",");
                    sb.append("\"detalle\":\"").append(escapeJson(a.getDetalle())).append("\",");
                    sb.append("\"productoId\":").append(a.getProductoId() != null ? a.getProductoId() : "null").append(",");
                    sb.append("\"ordenId\":").append(a.getOrdenId() != null ? a.getOrdenId() : "null").append(",");
                    sb.append("\"bodegaId\":").append(a.getBodegaId() != null ? a.getBodegaId() : "null");
                    sb.append("}");
                }
            }
            sb.append("],");

            // Acciones sugeridas
            sb.append("\"accionesSugeridas\":[");
            if (request.getAccionesSugeridas() != null) {
                for (int i = 0; i < request.getAccionesSugeridas().size(); i++) {
                    var a = request.getAccionesSugeridas().get(i);
                    if (i > 0) sb.append(",");
                    sb.append("{");
                    sb.append("\"tipo\":\"").append(a.getTipo()).append("\",");
                    sb.append("\"descripcion\":\"").append(escapeJson(a.getDescripcion())).append("\",");
                    sb.append("\"ordenId\":").append(a.getOrdenId() != null ? a.getOrdenId() : "null").append(",");
                    sb.append("\"productoId\":").append(a.getProductoId() != null ? a.getProductoId() : "null").append(",");
                    sb.append("\"bodegaId\":").append(a.getBodegaId() != null ? a.getBodegaId() : "null");
                    sb.append("}");
                }
            }
            sb.append("]}");
            json = sb.toString();
        } catch (Exception e) {
            json = null;
        }

        String username = UserContext.getUsername();
        Optional<ResumenPanel> existente = resumenPanelRepository.findByFecha(request.getFecha());

        ResumenPanel resumen;
        boolean esActualizacion = existente.isPresent();

        if (esActualizacion) {
            resumen = existente.get();
            resumen.setNarrativa(request.getNarrativa());
            resumen.setContenidoJson(json);
            resumen.setAutor(username);
        } else {
            resumen = ResumenPanel.builder()
                    .fecha(request.getFecha())
                    .narrativa(request.getNarrativa())
                    .contenidoJson(json)
                    .autor(username)
                    .build();
        }

        resumenPanelRepository.save(resumen);

        // Registrar auditoria
        try {
            Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);
            String tipoOperacion = esActualizacion ? "UPDATE" : "INSERT";
            auditoriaRepository.save(Auditoria.builder()
                    .tipoOperacion(TipoOperacion.valueOf(tipoOperacion))
                    .usuario(usuario)
                    .entidadAfectada("ResumenPanel")
                    .entidadId(resumen.getId())
                    .valoresNuevos(json)
                    .build());
        } catch (Exception ignored) {
        }
    }

    @Override
    public Optional<ResumenPanel> obtenerUltimoResumen() {
        return resumenPanelRepository.findTopByOrderByCreatedAtDesc();
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}