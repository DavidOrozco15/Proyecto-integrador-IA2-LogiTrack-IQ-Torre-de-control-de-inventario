package com.logitrack.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "resumen_panel")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumenPanel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "La fecha es obligatoria")
    @Column(nullable = false)
    private LocalDate fecha;

    @NotBlank(message = "La narrativa es obligatoria")
    @Size(min = 10, max = 2000, message = "La narrativa debe tener entre 10 y 2000 caracteres")
    @Column(nullable = false, length = 2000)
    private String narrativa;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}