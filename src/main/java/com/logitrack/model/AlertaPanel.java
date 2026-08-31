package com.logitrack.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "alertas_panel")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertaPanel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resumen_id", nullable = false)
    private ResumenPanel resumen;

    @NotNull(message = "La severidad es obligatoria")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SeveridadAlerta severidad;

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String titulo;

    @NotBlank(message = "El detalle es obligatorio")
    @Size(max = 500)
    @Column(nullable = false, length = 500)
    private String detalle;

    @Column(name = "producto_id")
    private Long productoId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}