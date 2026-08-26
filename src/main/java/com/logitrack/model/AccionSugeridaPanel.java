package com.logitrack.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "acciones_sugeridas_panel")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccionSugeridaPanel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resumen_id", nullable = false)
    private ResumenPanel resumen;

    @NotNull(message = "El tipo es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoAccionSugerida tipo;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 500)
    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(name = "orden_id")
    private Long ordenId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}